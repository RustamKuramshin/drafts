package com.rustam.dev;

class SegmentTree {
    int[] tree;
    int n;

    public SegmentTree(int[] arr) {
        this.n = arr.length;
        // Для упрощения реализации размер дерева берется как 4*n
        tree = new int[4 * n];
        buildTree(arr, 0, 0, n - 1);
    }

    // Метод для построения дерева
    private void buildTree(int[] arr, int treeIndex, int lo, int hi) {
        if (lo == hi) {
            tree[treeIndex] = arr[lo];
            return;
        }
        int mid = lo + (hi - lo) / 2;
        buildTree(arr, 2 * treeIndex + 1, lo, mid); // Построение левого поддерева
        buildTree(arr, 2 * treeIndex + 2, mid + 1, hi); // Построение правого поддерева
        tree[treeIndex] = Math.min(tree[2 * treeIndex + 1], tree[2 * treeIndex + 2]); // Обновление узла
    }

    // Метод для обновления элемента массива и дерева отрезков
    public void update(int index, int val) {
        updateHelper(0, 0, n - 1, index, val);
    }

    private void updateHelper(int treeIndex, int lo, int hi, int arrIndex, int val) {
        if (lo == hi) {
            tree[treeIndex] = val;
            return;
        }
        int mid = lo + (hi - lo) / 2;
        if (arrIndex <= mid) {
            updateHelper(2 * treeIndex + 1, lo, mid, arrIndex, val);
        } else {
            updateHelper(2 * treeIndex + 2, mid + 1, hi, arrIndex, val);
        }
        tree[treeIndex] = Math.min(tree[2 * treeIndex + 1], tree[2 * treeIndex + 2]);
    }

    // Метод для выполнения RMQ
    public int rmq(int qs, int qe) {
        return rmqHelper(0, 0, n - 1, qs, qe);
    }

    private int rmqHelper(int treeIndex, int lo, int hi, int qs, int qe) {
        if (qs <= lo && qe >= hi) { // Полное покрытие
            return tree[treeIndex];
        }
        if (qs > hi || qe < lo) { // Без покрытия
            return Integer.MAX_VALUE;
        }
        int mid = lo + (hi - lo) / 2;
        return Math.min(rmqHelper(2 * treeIndex + 1, lo, mid, qs, qe),
                rmqHelper(2 * treeIndex + 2, mid + 1, hi, qs, qe));
    }
}

// Пример использования
class Main {
    public static void main(String[] args) {
        int[] arr = {1, 3, 5, 7, 9, 11};
        SegmentTree st = new SegmentTree(arr);
        System.out.println("RMQ(1, 3): " + st.rmq(1, 3)); // Минимум в подотрезке [1, 3]
        st.update(1, -1); // Обновление элемента с индексом 1 на -1
        System.out.println("RMQ(1, 3): " + st.rmq(1, 3)); // Минимум в подотрезке [1, 3] после обновления
    }
}
