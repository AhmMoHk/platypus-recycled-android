package cs.System.Reflection;

public class TargetInvocationException extends cs.System.Exception {
    public TargetInvocationException(cs.System.Exception inner) { super("Exception has been thrown by the target of an invocation.", inner); }
}
