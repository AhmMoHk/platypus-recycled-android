package cs.System;

/** System.Exception. Java runtime exceptions are wrapped into the matching .NET types when caught. */
public class Exception extends RuntimeException {
    protected String msg;
    protected Exception inner;

    public Exception() { super(); }
    public Exception(String message) { super(message); msg = message; }
    public Exception(String message, Exception innerException) { super(message, innerException); msg = message; inner = innerException; }
    public Exception(String message, Throwable cause) { super(message, cause); msg = message; }

    /** base(message) called from a translated subclass constructor */
    public void $init(String message) { msg = message; }
    public void $init(String message, Exception innerException) { msg = message; inner = innerException; }

    public String get_Message() {
        if (msg != null) return msg;
        return "Exception of type '" + getClass().getSimpleName() + "' was thrown.";
    }
    @Override public String getMessage() { return get_Message(); }
    public Exception get_InnerException() { return inner; }
    public String get_StackTrace() {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement e : getStackTrace()) sb.append("  at ").append(e).append('\n');
        return sb.toString();
    }
    public String get_Source() { return "Platypus"; }
    @Override public String toString() { return getClass().getName().replace("cs.", "") + ": " + get_Message(); }

    public static Exception wrap(Throwable t) {
        if (t instanceof Exception) return (Exception) t;
        Exception e;
        if (t instanceof NullPointerException) e = new NullReferenceException(String.valueOf(t.getMessage()));
        else if (t instanceof ClassCastException) e = new InvalidCastException(String.valueOf(t.getMessage()));
        else if (t instanceof ArrayIndexOutOfBoundsException || t instanceof StringIndexOutOfBoundsException) e = new IndexOutOfRangeException(String.valueOf(t.getMessage()));
        else if (t instanceof ArithmeticException) e = new DivideByZeroException(String.valueOf(t.getMessage()));
        else if (t instanceof NumberFormatException) e = new FormatException(String.valueOf(t.getMessage()));
        else if (t instanceof StackOverflowError || t instanceof OutOfMemoryError) {
            if (t instanceof Error) throw (Error) t;
            e = new Exception(t.toString());
        } else e = new Exception(t.toString());
        e.initCause(t);
        e.setStackTrace(t.getStackTrace());
        return e;
    }
}
