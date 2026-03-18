package com.rustam.dev.dsa.graphs;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class GraphAdjacencyList {

    private final List<List<Integer>> adjList;

    public GraphAdjacencyList(int vertices) {
        adjList = new ArrayList<>(vertices);
        for (int i = 0; i < vertices; i++) {
            adjList.add(new LinkedList<>());
        }
    }

    public void addEdge(int src, int dest) {
        // Добавление ребра от src к dest
        adjList.get(src).add(dest);
        // Для ненаправленного графа, добавляем обратное ребро
        adjList.get(dest).add(src);
    }

    public void printGraph() {
        for (int i = 0; i < adjList.size(); i++) {
            System.out.println("Смежные вершины для вершины " + i + ": " + adjList.get(i));
        }
    }

    public static void main(String[] args) {
        GraphAdjacencyList graph = new GraphAdjacencyList(4);
        graph.addEdge(0, 1);
        graph.addEdge(0, 2);
        graph.addEdge(1, 2);
        graph.addEdge(2, 3);

        graph.printGraph();
    }
}

