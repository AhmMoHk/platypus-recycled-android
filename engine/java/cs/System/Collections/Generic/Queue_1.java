package cs.System.Collections.Generic;


public class Queue_1 implements IEnumerable_1, IReadOnlyCollection_1, cs.System.Collections.ICollection {
    final java.util.ArrayList<Object> q = new java.util.ArrayList<>();
    int head;
    int version;

    public Queue_1() {}
    public Queue_1(int capacity) {}
    public Queue_1(IEnumerable_1 src) { IEnumerator_1 e = src.GetEnumerator(); while (e.MoveNext()) Enqueue(e.get_Current()); }

    private void compact() { if (head > 32 && head * 2 > q.size()) { q.subList(0, head).clear(); head = 0; } }

    public int get_Count() { return q.size() - head; }
    public void Enqueue(Object v) { q.add(v); version++; }
    public Object Dequeue() {
        if (get_Count() == 0) throw new cs.System.InvalidOperationException("Queue empty.");
        Object v = q.get(head);
        q.set(head++, null);
        version++;
        compact();
        return v;
    }
    public Object Peek() {
        if (get_Count() == 0) throw new cs.System.InvalidOperationException("Queue empty.");
        return q.get(head);
    }
    public boolean TryDequeue(cs.rt.RefO r) { if (get_Count() == 0) { r.v = null; return false; } r.v = Dequeue(); return true; }
    public boolean TryPeek(cs.rt.RefO r) { if (get_Count() == 0) { r.v = null; return false; } r.v = Peek(); return true; }
    public boolean Contains(Object v) { for (int i = head; i < q.size(); i++) if (cs.System.ObjectX.Equals(q.get(i), v)) return true; return false; }
    public void Clear() { q.clear(); head = 0; version++; }
    public Object ToArray() { return q.subList(head, q.size()).toArray(); }
    Object[] snapshot() { return q.subList(head, q.size()).toArray(); }
    public Queue_1$Enumerator GetEnumerator() { return new Queue_1$Enumerator(this); }
}
