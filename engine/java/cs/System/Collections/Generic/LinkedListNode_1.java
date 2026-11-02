package cs.System.Collections.Generic;

public class LinkedListNode_1 {
    LinkedList_1 list;
    LinkedListNode_1 next, prev;
    Object value;
    public LinkedListNode_1() {}
    public LinkedListNode_1(Object v) { value = v; }
    public LinkedListNode_1 get_Next() { return next == null || next == list.head ? null : next; }
    public LinkedListNode_1 get_Previous() { return prev == null || this == list.head ? null : prev; }
    public Object get_Value() { return value; }
    public void set_Value(Object v) { value = v; }
    public LinkedList_1 get_List() { return list; }
}
