package cs.UnityEngine.Pool;

public interface IObjectPool_1 {
    int get_CountInactive();
    Object Get();
    void Release(Object o);
    void Clear();
}
