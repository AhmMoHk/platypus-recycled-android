"""
Mapping of .NET types / names to Java for the IL -> Java translator.

Conventions shared with the hand-written runtime (Java package `cs`):
  * every .NET namespace N maps to Java package `cs.N`; the global namespace maps to `cs.global`
  * nested types are top-level Java classes named Outer$Inner; generic arity `n becomes _n
  * primitives map to Java primitives (bool->boolean, char/u2->char, i1/u1->byte, i2->short, i4/u4->int,
    i8/u8->long, r4->float, r8->double, IntPtr->long), string->String, object->Object
  * enums are their underlying primitive; the enum class holds constants and an EnumInfo
  * structs are classes with value semantics: $copy() / $set(src) / $clear(), arrays via $newArray(n)
  * byrefs: struct byref = the struct object itself; other byrefs = cs.rt.RefX cells (field v)
  * generic methods take one leading cs.System.Type argument per method type parameter
  * game constructors are instance methods $ctor_<SimpleName>; external (runtime) types use Java constructors
"""
import re

JAVA_KEYWORDS = set('''abstract assert boolean break byte case catch char class const continue default do double else
enum extends final finally float for goto if implements import instanceof int interface long native new package private
protected public return short static strictfp super switch synchronized this throw throws transient try void volatile
while true false null var record yield sealed permits'''.split())

PRIM_J = {'void': 'void', 'bool': 'boolean', 'char': 'char', 'i1': 'byte', 'u1': 'byte', 'i2': 'short',
          'u2': 'char', 'i4': 'int', 'u4': 'int', 'i8': 'long', 'u8': 'long', 'r4': 'float', 'r8': 'double',
          'i': 'long', 'u': 'long', 'string': 'String', 'object': 'Object', 'typedref': 'Object'}
BOX_J = {'boolean': 'Boolean', 'char': 'Character', 'byte': 'Byte', 'short': 'Short', 'int': 'Integer',
         'long': 'Long', 'float': 'Float', 'double': 'Double'}
REF_CELL = {'boolean': 'cs.rt.RefZ', 'char': 'cs.rt.RefC', 'byte': 'cs.rt.RefB', 'short': 'cs.rt.RefS',
            'int': 'cs.rt.RefI', 'long': 'cs.rt.RefJ', 'float': 'cs.rt.RefF', 'double': 'cs.rt.RefD'}
JPRIMS = set(BOX_J)

# .NET types that map to special Java classes
RENAMES = {
    'System.Object': 'Object', 'System.String': 'String', 'System.ValueType': 'Object', 'System.Enum': 'Object',
    'System.Array': 'Object', 'System.Void': 'void',
    'System.MulticastDelegate': 'cs.System.Delegate', 'System.Delegate': 'cs.System.Delegate',
    'UnityEngine.Object': 'cs.UnityEngine.UObject',
}

# external value types: enums (underlying type) ...
EXT_ENUMS = {
    'System.StringSplitOptions': 'i4', 'System.StringComparison': 'i4', 'System.MidpointRounding': 'i4',
    'System.Globalization.NumberStyles': 'i4', 'System.Reflection.BindingFlags': 'i4', 'System.IO.SearchOption': 'i4',
    'System.AttributeTargets': 'i4', 'System.DayOfWeek': 'i4', 'System.DateTimeKind': 'i4',
    'System.Globalization.CompareOptions': 'i4', 'System.IO.FileMode': 'i4', 'System.IO.FileAccess': 'i4',
    'System.Environment/SpecialFolder': 'i4', 'System.TypeCode': 'i4', 'System.Base64FormattingOptions': 'i4',
    'UnityEngine.FilterMode': 'i4', 'UnityEngine.TextureWrapMode': 'i4', 'UnityEngine.TextureFormat': 'i4',
    'UnityEngine.SpriteMeshType': 'i4', 'UnityEngine.SpriteDrawMode': 'i4', 'UnityEngine.FullScreenMode': 'i4',
    'UnityEngine.CameraClearFlags': 'i4', 'UnityEngine.TransparencySortMode': 'i4', 'UnityEngine.CursorLockMode': 'i4',
    'UnityEngine.LogType': 'i4', 'UnityEngine.HideFlags': 'i4', 'UnityEngine.FontStyle': 'i4',
    'UnityEngine.TextAnchor': 'i4', 'UnityEngine.KeyCode': 'i4', 'UnityEngine.RenderMode': 'i4',
    'UnityEngine.CollisionDetectionMode2D': 'i4', 'UnityEngine.RigidbodyType2D': 'i4', 'UnityEngine.PhysicsShapeType2D': 'i4',
    'UnityEngine.Rendering.ShadowCastingMode': 'i4', 'UnityEngine.FindObjectsSortMode': 'i4',
    'UnityEngine.FindObjectsInactive': 'i4', 'UnityEngine.RuntimePlatform': 'i4', 'UnityEngine.SystemLanguage': 'i4',
    'UnityEngine.Networking.UnityWebRequest/Result': 'i4', 'UnityEngine.AudioType': 'i4', 'UnityEngine.SceneManagement.LoadSceneMode': 'i4',
    'UnityEngine.UI.Image/Type': 'i4', 'UnityEngine.UI.Image/FillMethod': 'i4', 'UnityEngine.UI.Navigation/Mode': 'i4',
    'UnityEngine.UI.AspectRatioFitter/AspectMode': 'i4', 'UnityEngine.RenderTextureFormat': 'i4',
    'UnityEngine.ScreenOrientation': 'i4', 'UnityEngine.DeviceType': 'i4', 'UnityEngine.AnimatorCullingMode': 'i4',
    'UnityEngine.SpriteSortPoint': 'i4', 'UnityEngine.SpriteMaskInteraction': 'i4', 'UnityEngine.Experimental.Rendering.GraphicsFormat': 'i4',
    'UnityEngine.ForceMode2D': 'i4', 'UnityEngine.RenderTextureReadWrite': 'i4', 'UnityEngine.Video.VideoAspectRatio': 'i4',
    'UnityEngine.Video.VideoRenderMode': 'i4', 'UnityEngine.ParticleSystemStopBehavior': 'i4',
    'UnityEngine.AsyncOperationPriority': 'i4', 'UnityEngine.ThreadPriority': 'i4',
    'TMPro.TextAlignmentOptions': 'i4', 'TMPro.HorizontalAlignmentOptions': 'i4', 'TMPro.VerticalAlignmentOptions': 'i4',
    'TMPro.TextOverflowModes': 'i4', 'TMPro.FontStyles': 'i4',
    'Rewired.ControllerType': 'i4', 'Rewired.InputActionEventType': 'i4', 'Rewired.UpdateLoopType': 'i4',
    'Rewired.InputActionType': 'i4', 'Rewired.ControllerElementType': 'i4', 'Rewired.Pole': 'i4',
    'Rewired.AxisRange': 'i4', 'Rewired.ModifierKeyFlags': 'i4', 'Rewired.KeyboardKeyCode': 'i4',
    'UnityEngine.EventSystems.PointerEventData/InputButton': 'i4', 'UnityEngine.EventSystems.MoveDirection': 'i4',
    'UnityEngine.UI.Selectable/Transition': 'i4', 'UnityEngine.UI.Slider/Direction': 'i4', 'UnityEngine.UI.Text': None,
    'Blackdrop.Platforms.PlatformType': 'i4', 'Blackdrop.Platforms.StoreLocation': 'i4',
    'MoonSharp.Interpreter.DataType': 'i4', 'MoonSharp.Interpreter.CoreModules': 'i4',
    'System.Threading.Tasks.TaskStatus': 'i4', 'System.Diagnostics.DebuggableAttribute/DebuggingModes': 'i4',
    'System.Runtime.CompilerServices.MethodImplOptions': 'i4', 'System.ConsoleColor': 'i4',
    'System.Globalization.UnicodeCategory': 'i4', 'System.IO.FileShare': 'i4', 'System.Text.NormalizationForm': 'i4',
    'System.Reflection.MemberTypes': 'i4', 'System.Reflection.MethodAttributes': 'i4', 'System.Reflection.FieldAttributes': 'i4',
    'System.Reflection.ParameterAttributes': 'i4', 'System.Reflection.TypeAttributes': 'i4', 'System.Reflection.PropertyAttributes': 'i4',
    'System.Runtime.InteropServices.CharSet': 'i4', 'System.Runtime.InteropServices.LayoutKind': 'i4',
    'System.Runtime.InteropServices.CallingConvention': 'i4', 'System.Runtime.InteropServices.GCHandleType': 'i4',
    'System.Security.Permissions.SecurityAction': 'i4', 'System.Text.RegularExpressions.RegexOptions': 'i4',
    'System.Threading.LazyThreadSafetyMode': 'i4', 'System.Linq.Expressions.ExpressionType': 'i4',
    'System.Runtime.CompilerServices.CompilationRelaxations': 'i4', 'System.ComponentModel.EditorBrowsableState': 'i4',
    'System.Diagnostics.DebuggerBrowsableState': 'i4', 'System.Globalization.DateTimeStyles': 'i4',
    'System.Diagnostics.SourceLevels': 'i4', 'System.IO.FileAttributes': 'i4', 'System.Net.HttpStatusCode': 'i4',
    'Blackdrop.Platforms.ActivityID': 'i4', 'Blackdrop.Platforms.ActivityOutcome': 'i4',
    'Blackdrop.Platforms.BasePlatformSystem/ContentInfo/DescFlags': 'i4', 'Blackdrop.Platforms.PlatformModel': 'i4',
    'Blackdrop.Util.Locale': 'i4', 'MoonSharp.Interpreter.InteropAccessMode': 'i4', 'UnityEngine.CursorMode': 'i4',
    'UnityEngine.RectTransform/Axis': 'i4', 'UnityEngine.RuntimeInitializeLoadType': 'i4', 'UnityEngine.TextAlignment': 'i4',
    'MoonSharp.Interpreter.SymbolRefType': 'i4', 'System.IO.SeekOrigin': 'i4', 'System.GCCollectionMode': 'i4',
    'System.UriKind': 'i4', 'System.Globalization.CultureTypes': 'i4', 'MoonSharp.Interpreter.CoroutineState': 'i4',
}
EXT_ENUMS = {k: v for k, v in EXT_ENUMS.items() if v}

# ... structs with value (copy) semantics ...
EXT_STRUCTS = {
    'UnityEngine.Vector2', 'UnityEngine.Vector3', 'UnityEngine.Vector4', 'UnityEngine.Vector2Int', 'UnityEngine.Vector3Int',
    'UnityEngine.Color', 'UnityEngine.Color32', 'UnityEngine.Rect', 'UnityEngine.RectInt', 'UnityEngine.Quaternion',
    'UnityEngine.Bounds', 'UnityEngine.Matrix4x4', 'UnityEngine.Keyframe', 'UnityEngine.GradientColorKey',
    'UnityEngine.GradientAlphaKey', 'UnityEngine.Resolution', 'UnityEngine.AnimatorStateInfo', 'UnityEngine.LayerMask',
    'UnityEngine.SceneManagement.Scene', 'UnityEngine.Ray', 'UnityEngine.Ray2D', 'UnityEngine.Plane', 'UnityEngine.RaycastHit2D',
    'UnityEngine.ContactPoint2D', 'UnityEngine.ContactFilter2D', 'UnityEngine.PhysicsShape2D', 'UnityEngine.Touch',
    'UnityEngine.UI.Navigation', 'UnityEngine.UI.ColorBlock', 'UnityEngine.UI.SpriteState', 'UnityEngine.RenderTextureDescriptor',
    'UnityEngine.Rendering.SphericalHarmonicsL2', 'UnityEngine.Hash128', 'UnityEngine.BoundsInt', 'UnityEngine.RangeInt',
    'UnityEngine.Pose', 'UnityEngine.UIVertex', 'UnityEngine.CombineInstance', 'System.DateTimeOffset',
    'Unity.Profiling.ProfilerMarker', 'Unity.Profiling.ProfilerMarker/AutoScope',
    'System.Guid', 'System.DateTime', 'System.TimeSpan', 'System.Decimal', 'System.Index', 'System.Range',
    'System.Nullable`1', 'System.ValueTuple`1', 'System.ValueTuple`2', 'System.ValueTuple`3', 'System.ValueTuple`4',
    'System.ValueTuple`5', 'System.ValueTuple`6', 'System.ValueTuple`7', 'System.ValueTuple`8', 'System.ArraySegment`1',
    'System.Span`1', 'System.ReadOnlySpan`1', 'System.Memory`1', 'System.RuntimeTypeHandle', 'System.RuntimeFieldHandle',
    'System.RuntimeMethodHandle', 'System.Runtime.InteropServices.GCHandle', 'System.Threading.CancellationToken',
    'System.Runtime.CompilerServices.TaskAwaiter', 'System.Runtime.CompilerServices.AsyncVoidMethodBuilder',
    'System.Runtime.CompilerServices.AsyncTaskMethodBuilder', 'System.Runtime.CompilerServices.AsyncTaskMethodBuilder`1',
    'System.Collections.DictionaryEntry', 'System.Threading.SpinLock', 'System.Numerics.BigInteger',
    'Rewired.InputActionEventData', 'Rewired.ControllerPollingInfo', 'MoonSharp.Interpreter.DynValue',
}
# ... and value types used like references (no copying needed: iterators, immutable pairs)
EXT_NOCOPY = {
    'System.Collections.Generic.List`1/Enumerator', 'System.Collections.Generic.Dictionary`2/Enumerator',
    'System.Collections.Generic.Dictionary`2/KeyCollection/Enumerator', 'System.Collections.Generic.Dictionary`2/ValueCollection/Enumerator',
    'System.Collections.Generic.HashSet`1/Enumerator', 'System.Collections.Generic.Queue`1/Enumerator',
    'System.Collections.Generic.Stack`1/Enumerator', 'System.Collections.Generic.LinkedList`1/Enumerator',
    'System.Collections.Generic.SortedDictionary`2/Enumerator', 'System.Collections.Generic.SortedList`2/Enumerator',
    'System.Collections.Generic.SortedSet`1/Enumerator',
    'System.Collections.Generic.KeyValuePair`2', 'UnityEngine.Transform/Enumerator',
}

# external delegate types: Invoke signatures as (ret, [params]) using md type tuples (V = class generic param)
def _V(n):
    return ('V', n)
_VOID = ('P', 'void')
_OBJ = ('P', 'object')
_STR = ('P', 'string')
_BOOL = ('P', 'bool')
EXT_DELEGATES = {}
for n in range(0, 9):
    EXT_DELEGATES['System.Action' + (f'`{n}' if n else '')] = (_VOID, [_V(i) for i in range(n)])
    EXT_DELEGATES[f'System.Func`{n + 1}'] = (_V(n), [_V(i) for i in range(n)])
for n in range(0, 5):
    EXT_DELEGATES['UnityEngine.Events.UnityAction' + (f'`{n}' if n else '')] = (_VOID, [_V(i) for i in range(n)])
EXT_DELEGATES.update({
    'System.Predicate`1': (_BOOL, [_V(0)]),
    'System.Comparison`1': (('P', 'i4'), [_V(0), _V(0)]),
    'System.Converter`2': (_V(1), [_V(0)]),
    'System.EventHandler': (_VOID, [_OBJ, ('N', 'System.EventArgs', False)]),
    'System.EventHandler`1': (_VOID, [_OBJ, _V(0)]),
    'System.AsyncCallback': (_VOID, [('N', 'System.IAsyncResult', False)]),
    'System.Threading.ThreadStart': (_VOID, []),
    'System.Text.RegularExpressions.MatchEvaluator': (_STR, [('N', 'System.Text.RegularExpressions.Match', False)]),
    'System.Threading.ParameterizedThreadStart': (_VOID, [_OBJ]),
    'System.Threading.WaitCallback': (_VOID, [_OBJ]),
    'System.Threading.TimerCallback': (_VOID, [_OBJ]),
    'System.UnhandledExceptionEventHandler': (_VOID, [_OBJ, ('N', 'System.UnhandledExceptionEventArgs', False)]),
    'System.ResolveEventHandler': (('N', 'System.Reflection.Assembly', False), [_OBJ, ('N', 'System.ResolveEventArgs', False)]),
    'UnityEngine.Application/LogCallback': (_VOID, [_STR, _STR, ('N', 'UnityEngine.LogType', True)]),
    'UnityEngine.Application/AdvertisingIdentifierCallback': (_VOID, [_STR, _BOOL, _STR]),
    'UnityEngine.Camera/CameraCallback': (_VOID, [('N', 'UnityEngine.Camera', False)]),
    'UnityEngine.Canvas/WillRenderCanvases': (_VOID, []),
    'UnityEngine.UIElements.EventCallback`1': (_VOID, [_V(0)]),
    'UnityEngine.Font/<>': (_VOID, []),
    'UnityEngine.Font/FontTextureRebuildCallback': (_VOID, []),
    'UnityEngine.Video.VideoPlayer/EventHandler': (_VOID, [('N', 'UnityEngine.Video.VideoPlayer', False)]),
    'UnityEngine.Video.VideoPlayer/ErrorEventHandler': (_VOID, [('N', 'UnityEngine.Video.VideoPlayer', False), _STR]),
    'UnityEngine.AudioClip/PCMReaderCallback': (_VOID, [('A', ('P', 'r4'))]),
    'UnityEngine.AudioClip/PCMSetPositionCallback': (_VOID, [('P', 'i4')]),
    'UnityEngine.UI.InputField/OnValidateInput': (('P', 'char'), [_STR, ('P', 'i4'), ('P', 'char')]),
    'UnityEngine.RectTransform/ReapplyDrivenProperties': (_VOID, [('N', 'UnityEngine.RectTransform', False)]),
    'UnityEngine.Display/DisplaysUpdatedDelegate': (_VOID, []),
    'UnityEngine.SceneManagement.SceneManager/<sceneLoaded>': (_VOID, []),
    'Rewired.PlayerActiveControllerChangedDelegate': (_VOID, [('N', 'Rewired.Player', False), ('N', 'Rewired.Controller', False)]),
    'Rewired.ControllerHelper/LastActiveControllerChangedDelegate': (_VOID, [('N', 'Rewired.Player', False), ('N', 'Rewired.Controller', False)]),
})


def sanitize(s):
    out = []
    for ch in s:
        if ch.isalnum() and ch.isascii() or ch in '_$':
            out.append(ch)
        elif ch == '`':
            out.append('_')
        elif ch in '<>':
            out.append('$')
        else:
            out.append('_')
    r = ''.join(out)
    if not r or r[0].isdigit():
        r = '_' + r
    if r in JAVA_KEYWORDS:
        r += '_'
    return r


def jclass_name(fullname):
    """Java fully-qualified class name of a .NET type name (no generic args)."""
    if fullname in RENAMES:
        return RENAMES[fullname]
    parts = fullname.split('/')
    top = parts[0]
    ns, _, simple = top.rpartition('.')
    pkg = 'cs.' + '.'.join(sanitize(p) for p in ns.split('.')) if ns else 'cs.global'
    cls = '$'.join(sanitize(p) for p in [simple] + parts[1:])
    return pkg + '.' + cls


def jsimple(fq):
    return fq.rsplit('.', 1)[-1]


def jstr_literal(s):
    out = ['"']
    for ch in s:
        o = ord(ch)
        if ch == '"':
            out.append('\\"')
        elif ch == '\\':
            out.append('\\\\')
        elif ch == '\n':
            out.append('\\n')
        elif ch == '\r':
            out.append('\\r')
        elif ch == '\t':
            out.append('\\t')
        elif o < 0x20 or o == 0x7f:
            out.append('\\%03o' % o)
        elif o > 0x7e:
            out.append('\\u%04x' % o)
        else:
            out.append(ch)
    out.append('"')
    return ''.join(out)


def jint(v):
    v = int(v)
    v &= 0xffffffff
    if v >= 0x80000000:
        v -= 0x100000000
    if v == -0x80000000:
        return 'Integer.MIN_VALUE'
    return str(v) if v >= 0 else '(' + str(v) + ')'


def jlong(v):
    v = int(v) & 0xffffffffffffffff
    if v >= 0x8000000000000000:
        v -= 0x10000000000000000
    if v == -0x8000000000000000:
        return 'Long.MIN_VALUE'
    return (str(v) + 'L') if v >= 0 else '(' + str(v) + 'L)'


def jfloat(arg):
    import math, struct
    _, f, raw = arg
    if math.isnan(f):
        return 'Float.NaN'
    if math.isinf(f):
        return 'Float.POSITIVE_INFINITY' if f > 0 else 'Float.NEGATIVE_INFINITY'
    if f == 0 and (raw >> 31):
        return '(-0.0f)'
    # exact representation via hex float to avoid rounding issues
    r = repr(f)
    if struct.unpack('<f', struct.pack('<f', float(r)))[0] == f:
        s = r + 'f'
    else:
        s = float.hex(f) + 'f'
    return s if f >= 0 else '(' + s + ')'


def jdouble(arg):
    import math
    _, f, raw = arg
    if math.isnan(f):
        return 'Double.NaN'
    if math.isinf(f):
        return 'Double.POSITIVE_INFINITY' if f > 0 else 'Double.NEGATIVE_INFINITY'
    if f == 0 and (raw >> 63):
        return '(-0.0d)'
    s = repr(f) + 'd'
    return s if f >= 0 else '(' + s + ')'


# ---------------------------------------------------------------- reference-assembly index (Unity/Rewired/Blackdrop)
import json as _json, os as _os


def _tup(x):
    return tuple(_tup(i) for i in x) if isinstance(x, list) else x


try:
    EXT_INDEX = _json.load(open(_os.path.join(_os.path.dirname(__file__), 'ext_index.json')))
except OSError:
    EXT_INDEX = {}
for _n, _e in EXT_INDEX.items():
    if _n in EXT_ENUMS or _n in EXT_STRUCTS or _n in EXT_NOCOPY or _n in EXT_DELEGATES:
        continue
    if _e['k'] == 'enum':
        EXT_ENUMS[_n] = _e['ut']
    elif _e['k'] == 'struct':
        EXT_STRUCTS.add(_n)
    elif _e['k'] == 'delegate' and 'sig' in _e:
        EXT_DELEGATES[_n] = (_tup(_e['sig'][0]), [_tup(p) for p in _e['sig'][1]])
