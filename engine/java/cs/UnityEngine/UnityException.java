package cs.UnityEngine;

public class UnityException extends cs.System.SystemException {
    public UnityException() { super("A Unity Runtime error occurred!"); }
    public UnityException(String m) { super(m); }
    public UnityException(String m, cs.System.Exception inner) { super(m, inner); }
}
