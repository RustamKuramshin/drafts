package com.rustam.dev.leetcode.p237;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;

public class Solution {

    private static class ListNode {

        int val;
        ListNode next;

        ListNode(int x) {
            this.val = x;
        }
    }

    private class ObjectFinalizer extends PhantomReference<Object> {

        public ObjectFinalizer(Object referent, ReferenceQueue<? super Object> q) {
            super(referent, q);
        }

        public void finalizeResources() {
            System.out.println("clearing ...");
        }
    }

    public void deleteNode(ListNode node) {

        ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();
        ObjectFinalizer reference = new ObjectFinalizer(node.next, referenceQueue);

        node.val = node.next.val;
        node.next = node.next.next;
        System.gc();

        Reference<?> referenceFromQueue;
        System.out.println(reference.isEnqueued());
        while ((referenceFromQueue = referenceQueue.poll()) != null) {
            ((ObjectFinalizer)referenceFromQueue).finalizeResources();
            referenceFromQueue.clear();
        }
    }

    public static void main(String[] args) throws InterruptedException {

        ListNode ln = new ListNode(-8);
        ln.next = new ListNode(-5);
        ln.next.next = new ListNode(7);
        ln.next.next.next = new ListNode(8);
        ln.next.next.next.next = new ListNode(10);

        Solution s = new Solution();

        s.deleteNode(ln.next);
    }
}
