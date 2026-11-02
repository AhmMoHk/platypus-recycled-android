package cs.System.Collections;

public interface IEnumerator {
    boolean MoveNext();
    Object get_Current();
    void Reset();
}
