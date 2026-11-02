package cs.System.Reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

/** Field reflection over translated classes; .NET field types come from the class's $fmeta table. */
public class FieldInfo extends MemberInfo {
    protected Field field;
    protected String typeName;
    protected int flags;            // 1 static, 2 public, 4 initonly
    private cs.System.Type ftype;

    public FieldInfo() {}

    FieldInfo(cs.System.Type declaring, Field f, String csName, String typeName, int flags) {
        this.declaring = declaring;
        this.field = f;
        this.name = csName;
        this.typeName = typeName;
        this.flags = flags;
        this.attrKey = "F:" + csName;
        f.setAccessible(true);
    }

    /** BindingFlags: Instance=4 Static=8 Public=16 NonPublic=32, DeclaredOnly=2 */
    public static void collect(cs.System.Type t, int bf, ArrayList<FieldInfo> out) {
        Class<?> c = t.cls;
        boolean first = true;
        while (c != null && c != Object.class) {
            Object[] meta = null;
            try {
                Field m = c.getDeclaredField("$fmeta");
                meta = (Object[]) m.get(null);
            } catch (ReflectiveOperationException ignored) {
            }
            if (meta != null) {
                for (int i = 0; i + 3 < meta.length; i += 4) {
                    int fl = (Integer) meta[i + 3];
                    boolean st = (fl & 1) != 0, pub = (fl & 2) != 0;
                    if (st && (bf & 8) == 0) continue;
                    if (!st && (bf & 4) == 0) continue;
                    if (pub && (bf & 16) == 0) continue;
                    if (!pub && (bf & 32) == 0) continue;
                    if (!first && st) continue;              // statics of base classes are not returned
                    if (!first && !pub) continue;            // private fields of base classes are not visible
                    try {
                        Field f = c.getDeclaredField((String) meta[i]);
                        out.add(new FieldInfo(cs.System.Type.of(c), f, (String) meta[i + 1], (String) meta[i + 2], fl));
                    } catch (NoSuchFieldException ignored) {
                    }
                }
            } else {
                for (Field f : c.getDeclaredFields()) {
                    int mod = f.getModifiers();
                    if (f.getName().startsWith("$")) continue;
                    boolean st = Modifier.isStatic(mod), pub = Modifier.isPublic(mod);
                    if (st && (bf & 8) == 0 || !st && (bf & 4) == 0 || pub && (bf & 16) == 0 || !pub && (bf & 32) == 0) continue;
                    out.add(new FieldInfo(cs.System.Type.of(c), f, f.getName(), null, (st ? 1 : 0) | (pub ? 2 : 0)));
                }
            }
            if ((bf & 2) != 0) break;
            c = c.getSuperclass();
            first = false;
        }
    }

    public cs.System.Type get_FieldType() {
        if (ftype == null) {
            ftype = typeName != null ? cs.System.Reflection.Assembly.resolveType(typeName) : cs.System.Type.of(field.getType());
            if (ftype == null) ftype = cs.System.Type.of(field.getType());
        }
        return ftype;
    }

    public boolean get_IsStatic() { return (flags & 1) != 0; }
    public boolean get_IsPublic() { return (flags & 2) != 0; }
    public boolean get_IsInitOnly() { return (flags & 4) != 0; }
    public boolean get_IsLiteral() { return false; }
    public boolean get_IsPrivate() { return (flags & 2) == 0; }
    public boolean get_IsFamily() { return false; }
    public boolean get_IsAssembly() { return false; }
    public boolean get_IsFamilyOrAssembly() { return false; }
    public boolean get_IsFamilyAndAssembly() { return false; }
    public boolean get_IsSpecialName() { return false; }
    public static boolean op_Equality(FieldInfo a, FieldInfo b) { return a == null ? b == null : a.equals(b); }
    public static boolean op_Inequality(FieldInfo a, FieldInfo b) { return !op_Equality(a, b); }
    @Override public boolean equals(Object o) { return o instanceof FieldInfo && ((FieldInfo) o).field.equals(field); }
    @Override public int hashCode() { return field.hashCode(); }

    /** Value boxed the .NET way (enums keep their type, structs are copied). */
    public Object GetValue(Object obj) {
        try {
            Object v = field.get(get_IsStatic() ? null : obj);
            cs.System.Type ft = get_FieldType();
            if (ft.enumInfo != null && v instanceof Number) return ft.enumInfo.box(((Number) v).longValue());
            if (v instanceof cs.rt.Struct) return ((cs.rt.Struct) v).$copy();
            if (ft == cs.System.Type.UINT16 && v instanceof Character) return v;
            return v;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void SetValue(Object obj, Object value) {
        try {
            Object target = get_IsStatic() ? null : obj;
            Class<?> ft = field.getType();
            if (ft.isPrimitive()) {
                if (ft == int.class) field.setInt(target, cs.rt.Rt.unboxI(value));
                else if (ft == float.class) field.setFloat(target, cs.rt.Rt.unboxF(value));
                else if (ft == boolean.class) field.setBoolean(target, cs.rt.Rt.unboxZ(value));
                else if (ft == long.class) field.setLong(target, cs.rt.Rt.unboxJ(value));
                else if (ft == double.class) field.setDouble(target, cs.rt.Rt.unboxD(value));
                else if (ft == char.class) field.setChar(target, cs.rt.Rt.unboxC(value));
                else if (ft == byte.class) field.setByte(target, cs.rt.Rt.unboxB(value));
                else if (ft == short.class) field.setShort(target, cs.rt.Rt.unboxS(value));
            } else if (cs.rt.Struct.class.isAssignableFrom(ft) && value != null) {
                ((cs.rt.Struct) field.get(target)).$set(value);
            } else {
                field.set(target, value);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public String toString() { return get_FieldType() + " " + name; }
}
