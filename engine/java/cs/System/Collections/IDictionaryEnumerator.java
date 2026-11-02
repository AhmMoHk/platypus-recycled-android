package cs.System.Collections;

public interface IDictionaryEnumerator extends IEnumerator {
    Object get_Key();
    Object get_Value();
    DictionaryEntry get_Entry();
}
