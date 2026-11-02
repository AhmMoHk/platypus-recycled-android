package cs.System.Collections.Generic;

/** Circular doubly linked list like .NET's. */
public class LinkedList_1 implements ICollection_1 {
    LinkedListNode_1 head;
    int count, version;

    public LinkedList_1() {}

    public int get_Count() { return count; }
    public LinkedListNode_1 get_First() { return head; }
    public LinkedListNode_1 get_Last() { return head == null ? null : head.prev; }

    private void insertBefore(LinkedListNode_1 node, LinkedListNode_1 n) {
        n.list = this;
        n.next = node;
        n.prev = node.prev;
        node.prev.next = n;
        node.prev = n;
        count++;
        version++;
    }

    private void insertEmpty(LinkedListNode_1 n) {
        n.list = this;
        n.next = n;
        n.prev = n;
        head = n;
        count++;
        version++;
    }

    public LinkedListNode_1 AddLast(Object v) {
        LinkedListNode_1 n = new LinkedListNode_1(v);
        if (head == null) insertEmpty(n); else insertBefore(head, n);
        return n;
    }
    public void AddLast(LinkedListNode_1 n) { if (head == null) insertEmpty(n); else insertBefore(head, n); }
    public LinkedListNode_1 AddFirst(Object v) {
        LinkedListNode_1 n = new LinkedListNode_1(v);
        if (head == null) insertEmpty(n); else { insertBefore(head, n); head = n; }
        return n;
    }
    public LinkedListNode_1 AddBefore(LinkedListNode_1 node, Object v) {
        LinkedListNode_1 n = new LinkedListNode_1(v);
        insertBefore(node, n);
        if (node == head) head = n;
        return n;
    }
    public LinkedListNode_1 AddAfter(LinkedListNode_1 node, Object v) {
        LinkedListNode_1 n = new LinkedListNode_1(v);
        insertBefore(node.next, n);
        return n;
    }
    public void Add(Object v) { AddLast(v); }

    public void Remove(LinkedListNode_1 n) {
        if (n.list != this) throw new cs.System.InvalidOperationException("The LinkedList node does not belong to current LinkedList.");
        if (n.next == n) head = null;
        else {
            n.next.prev = n.prev;
            n.prev.next = n.next;
            if (head == n) head = n.next;
        }
        n.list = null;
        n.next = n.prev = null;
        count--;
        version++;
    }
    public boolean Remove(Object v) {
        LinkedListNode_1 n = Find(v);
        if (n == null) return false;
        Remove(n);
        return true;
    }
    public void RemoveFirst() { if (head == null) throw new cs.System.InvalidOperationException("The LinkedList is empty."); Remove(head); }
    public void RemoveLast() { if (head == null) throw new cs.System.InvalidOperationException("The LinkedList is empty."); Remove(head.prev); }
    public LinkedListNode_1 Find(Object v) {
        LinkedListNode_1 n = head;
        if (n == null) return null;
        do { if (cs.System.ObjectX.Equals(n.value, v)) return n; n = n.next; } while (n != head);
        return null;
    }
    public boolean Contains(Object v) { return Find(v) != null; }
    public void Clear() {
        LinkedListNode_1 n = head;
        while (n != null) { LinkedListNode_1 x = n; n = n.get_Next(); x.list = null; x.next = x.prev = null; }
        head = null;
        count = 0;
        version++;
    }

    public IEnumerator_1 GetEnumerator() {
        final java.util.ArrayList<Object> items = new java.util.ArrayList<>();
        LinkedListNode_1 n = head;
        if (n != null) do { items.add(n.value); n = n.next; } while (n != head);
        return new IEnumerator_1() {
            int i = -1;
            public boolean MoveNext() { if (i + 1 < items.size()) { i++; return true; } i = items.size(); return false; }
            public Object get_Current() { return i >= 0 && i < items.size() ? items.get(i) : null; }
            public void Reset() { i = -1; }
            public void Dispose() {}
        };
    }
}
