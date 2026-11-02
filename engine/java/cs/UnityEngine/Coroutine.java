package cs.UnityEngine;

import java.util.ArrayList;

/** A running coroutine: a stack of IEnumerators plus the current wait condition. */
public final class Coroutine extends YieldInstruction {
    static final int FRAME = 0, TIME = 1, REALTIME = 2, FIXED = 3, ENDFRAME = 4, CO = 5, CUSTOM = 6, ASYNC = 7;
    final MonoBehaviour owner;
    final ArrayList<cs.System.Collections.IEnumerator> stack = new ArrayList<>(2);
    int kind;
    Object waitFor;
    int waitFrame;
    double until;
    boolean done;

    Coroutine(MonoBehaviour owner, cs.System.Collections.IEnumerator e) {
        this.owner = owner;
        stack.add(e);
    }
}
