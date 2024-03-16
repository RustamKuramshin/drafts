package com.rustam.dev.graph;

public class GraphAdjacencyMatrix {
    private final int V; // Количество вершин
    private final int[][] adjMatrix; // Матрица смежности

    // Конструктор
    public GraphAdjacencyMatrix(int V) {
        this.V = V;
        adjMatrix = new int[V][V]; // Инициализация матрицы смежности
    }

    // Добавление ребра
    public void addEdge(int v, int w) {
        adjMatrix[v][w] = 1;
        adjMatrix[w][v] = 1; // Для ненаправленного графа
    }

    // Вывод матрицы смежности
    public void printAdjMatrix() {
        System.out.println("Матрица смежности:");
        for (int i = 0; i < V; i++) {
            for (int j = 0; j < V; j++) {
                System.out.print(adjMatrix[i][j] + " ");
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        GraphAdjacencyMatrix g = new GraphAdjacencyMatrix(4);

        g.addEdge(0, 1);
        g.addEdge(0, 2);
        g.addEdge(1, 2);
        g.addEdge(2, 3);

        g.printAdjMatrix();
    }
}
