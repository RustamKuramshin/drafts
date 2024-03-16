package com.rustam.dev.dsa;

public class UnionFind {

    private final int[] parent; // массив для хранения родителя каждого элемента
    private final int[] rank;   // ранг (или глубина) каждого дерева

    // Конструктор
    public UnionFind(int size) {
        parent = new int[size];
        rank = new int[size];

        for (int i = 0; i < size; i++) {
            parent[i] = i; // в начале каждый элемент является представителем своего собственного множества
            rank[i] = 0;   // ранг каждого дерева изначально 0
        }
    }

    // Метод для нахождения корня (представителя) множества, содержащего элемент x
    public int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]); // сжатие пути
        }
        return parent[x];
    }

    // Метод для объединения двух подмножеств
    public void union(int x, int y) {
        int xRoot = find(x);
        int yRoot = find(y);

        if (xRoot == yRoot) return; // Элементы уже находятся в одном множестве

        // Объединяем меньшее дерево под корнем большего
        if (rank[xRoot] < rank[yRoot]) {
            parent[xRoot] = yRoot;
        } else if (rank[yRoot] < rank[xRoot]) {
            parent[yRoot] = xRoot;
        } else {
            parent[yRoot] = xRoot;
            rank[xRoot] = rank[xRoot] + 1;
        }
    }

    public static void main(String[] args) {
        UnionFind uf = new UnionFind(10); // Создаем UFDS размером 10

        // Объединяем элементы в множества
        uf.union(1, 2);
        uf.union(2, 3);
        uf.union(4, 5);
        uf.union(6, 7);
        uf.union(5, 6);
        uf.union(3, 7);

        // Проверяем, находятся ли элементы в одном множестве
        System.out.println(uf.find(1) == uf.find(5)); // true, так как 1 и 5 соединены через другие элементы
        System.out.println(uf.find(1) == uf.find(8)); // false, так как 1 и 8 находятся в разных множествах
    }
}

