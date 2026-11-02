package cs.UnityEngine;

public interface ISerializationCallbackReceiver {
    void OnBeforeSerialize();
    void OnAfterDeserialize();
}
