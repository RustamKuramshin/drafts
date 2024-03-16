package com.rustam.dev.graphs;

import java.util.ArrayList;
import java.util.List;

public class GraphListOfEdges {
    static class Edge {
        int src, dest;

        Edge(int src, int dest) {
            this.src = src;
            this.dest = dest;
        }
    }

    List<Edge> edges;

    public GraphListOfEdges() {
        edges = new ArrayList<>();
    }

    public void addEdge(int src, int dest) {
        edges.add(new Edge(src, dest));
    }

    public void printGraph() {
        for (Edge edge : edges) {
            System.out.println(edge.src + " -> " + edge.dest);
        }
    }

    public static void main(String[] args) {
        GraphListOfEdges graph = new GraphListOfEdges();
        graph.addEdge(0, 1);
        graph.addEdge(0, 2);
        graph.addEdge(1, 2);
        graph.addEdge(2, 3);

        graph.printGraph();
    }
}

