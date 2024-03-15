package com.rustam.dev;

// Используем UFDS для эффективного объединения вершин в компоненты связности и определения общего количества таких компонент.
public class GraphComponents {
    private final int[] parent;
    private final int[] rank;
    private int count; // Количество компонент связности

    public GraphComponents(int N) {
        parent = new int[N];
        rank = new int[N];
        count = N; // Изначально каждая вершина - отдельная компонента связности
        for (int i = 0; i < N; i++) {
            parent[i] = i;
            rank[i] = 0;
        }
    }

    public int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]);
        }
        return parent[x];
    }

    public void union(int x, int y) {
        int xRoot = find(x);
        int yRoot = find(y);
        if (xRoot != yRoot) {
            if (rank[xRoot] < rank[yRoot]) {
                parent[xRoot] = yRoot;
            } else if (rank[yRoot] < rank[xRoot]) {
                parent[yRoot] = xRoot;
            } else {
                parent[yRoot] = xRoot;
                rank[xRoot]++;
            }
            count--; // Уменьшаем количество компонент связности
        }
    }

    public int getCount() {
        return count;
    }

    public static void main(String[] args) {
        int N = 5; // Пример: 5 вершин
        int[][] edges = {{0, 1}, {1, 2}, {3, 4}}; // Пример: рёбра графа

        GraphComponents graph = new GraphComponents(N);

        for (int[] edge : edges) {
            graph.union(edge[0], edge[1]);
        }

        System.out.println("Количество компонент связности: " + graph.getCount());
    }
}

