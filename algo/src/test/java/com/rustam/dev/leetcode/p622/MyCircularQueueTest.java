package com.rustam.dev.leetcode.p622;

import com.rustam.dev.leetcode.node.p622.IMyCircularQueue;
import com.rustam.dev.leetcode.node.p622.linkedlist.MyCircularQueue;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@DisplayName("Тестирования круговой очереди")
public class MyCircularQueueTest {

    private enum QueueType {
        ARRAY, NODE
    }

    private static class MyCircularQueueFactory {

        public static IMyCircularQueue getMyCircularQueue(QueueType type, int initSize) {
            if (type == QueueType.ARRAY) {
                return new com.rustam.dev.leetcode.node.p622.array.MyCircularQueue(initSize);
            } else if (type == QueueType.NODE) {
                return new MyCircularQueue(initSize);
            }
            return null;
        }
    }

    @Test
    public void test_size_3() {

        var rb = MyCircularQueueFactory.getMyCircularQueue(QueueType.ARRAY,3);

        assertTrue(rb.isEmpty());
        assertFalse(rb.isFull());

        assertEquals(-1, rb.Front());
        assertEquals(-1, rb.Rear());

        assertTrue(rb.enQueue(1));
        assertTrue(rb.enQueue(2));
        assertTrue(rb.enQueue(3));
        assertFalse(rb.enQueue(4));

        assertEquals(3, rb.Rear());
        assertTrue(rb.isFull());

        assertTrue(rb.deQueue());
        assertTrue(rb.enQueue(4));

        assertEquals(4, rb.Rear());
    }

    @Test
    public void test_size_1() {

        var rb = MyCircularQueueFactory.getMyCircularQueue(QueueType.ARRAY,1);

        assertTrue(rb.isEmpty());
        assertFalse(rb.isFull());

        assertEquals(-1, rb.Front());
        assertEquals(-1, rb.Rear());

        assertTrue(rb.enQueue(1));
        assertFalse(rb.enQueue(2));
        assertTrue(rb.deQueue());
        assertEquals(-1, rb.Front());
        assertEquals(-1, rb.Rear());
    }

    @Test
    public void test_size_2_full_life_cycle() {

        var rb = MyCircularQueueFactory.getMyCircularQueue(QueueType.ARRAY,2);

        assertTrue(rb.isEmpty());
        assertFalse(rb.isFull());

        assertEquals(-1, rb.Front());
        assertEquals(-1, rb.Rear());

        assertTrue(rb.enQueue(1));
        assertEquals(1, rb.Front());
        assertEquals(1, rb.Rear());
        assertFalse(rb.isEmpty());
        assertFalse(rb.isFull());

        assertTrue(rb.enQueue(2));
        assertEquals(1, rb.Front());
        assertEquals(2, rb.Rear());

        assertFalse(rb.enQueue(2));

        assertTrue(rb.deQueue());
        assertEquals(2, rb.Front());
        assertEquals(2, rb.Rear());

        assertTrue(rb.deQueue());
        assertEquals(-1, rb.Front());
        assertEquals(-1, rb.Rear());

        assertTrue(rb.isEmpty());
        assertFalse(rb.isFull());

        assertFalse(rb.deQueue());

        // check
        assertTrue(rb.isEmpty());
        assertFalse(rb.isFull());

        assertEquals(-1, rb.Front());
        assertEquals(-1, rb.Rear());

        assertTrue(rb.enQueue(1));
        assertEquals(1, rb.Front());
        assertEquals(1, rb.Rear());
        assertFalse(rb.isEmpty());
        assertFalse(rb.isFull());

        assertTrue(rb.enQueue(2));
        assertEquals(1, rb.Front());
        assertEquals(2, rb.Rear());

        assertFalse(rb.enQueue(2));

        assertTrue(rb.deQueue());
        assertEquals(2, rb.Front());
        assertEquals(2, rb.Rear());

        assertTrue(rb.deQueue());
        assertEquals(-1, rb.Front());
        assertEquals(-1, rb.Rear());

        assertTrue(rb.isEmpty());
        assertFalse(rb.isFull());

    }

    @Test
    public void testcase_40() {

        var rb = MyCircularQueueFactory.getMyCircularQueue(QueueType.ARRAY,6);

        assertTrue(rb.enQueue(6));
        assertEquals(6, rb.Rear());
        assertEquals(6, rb.Rear());
        assertTrue(rb.deQueue());
        assertTrue(rb.enQueue(5));
        assertEquals(5, rb.Rear());
        assertTrue(rb.deQueue());

        assertEquals(-1, rb.Front());
        assertFalse(rb.deQueue());
        assertFalse(rb.deQueue());
        assertFalse(rb.deQueue());
    }
}