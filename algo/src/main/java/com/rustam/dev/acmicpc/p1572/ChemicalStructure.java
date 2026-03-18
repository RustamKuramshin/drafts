package com.rustam.dev.acmicpc.p1572;

import java.util.*;

public class ChemicalStructure {

    private static class Molecule {
        String[] connectors = new String[4]; // To store the connectors in clockwise order

        public Molecule(String description) {
            for (int i = 0; i < 4; i++) {
                connectors[i] = description.substring(i * 2, i * 2 + 2);
            }
        }

        // Check if this molecule can connect with another molecule
        public boolean canConnectWith(Molecule other) {
            for (String connector : connectors) {
                for (String otherConnector : other.connectors) {
                    // Check for compatible connectors (excluding '00')
                    if (!"00".equals(connector) && !"00".equals(otherConnector) &&
                            connector.charAt(0) == otherConnector.charAt(0) &&
                            connector.charAt(1) != otherConnector.charAt(1)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextInt()) {
            int n = scanner.nextInt(); // Number of molecule types
            Molecule[] molecules = new Molecule[n];
            scanner.nextLine(); // Consume newline
            String[] descriptions = scanner.nextLine().split(" ");
            for (int i = 0; i < n; i++) {
                molecules[i] = new Molecule(descriptions[i]);
            }

            // Build the graph and check for cycles
            boolean unbounded = checkForUnboundedStructure(molecules);

            System.out.println(unbounded ? "unbounded" : "bounded");
        }
        scanner.close();
    }

    static class Graph {
        private int V; // Number of vertices
        List<List<Integer>> adjList; // Adjacency List

        public Graph(int V) {
            this.V = V;
            adjList = new ArrayList<>();
            for (int i = 0; i < V; i++) {
                adjList.add(new ArrayList<>());
            }
        }

        // Function to add an edge into the graph
        void addEdge(int v, int w) {
            adjList.get(v).add(w); // Add w to v's list.
        }

        // A recursive function used by DFS
        private boolean DFSUtil(int v, boolean[] visited, boolean[] recStack) {
            // Mark the current node as visited and part of recursion stack
            if (recStack[v]) return true; // Cycle detected
            if (visited[v]) return false;

            visited[v] = true;
            recStack[v] = true;

            List<Integer> children = adjList.get(v);
            for (Integer c : children)
                if (DFSUtil(c, visited, recStack))
                    return true;

            recStack[v] = false; // remove the vertex from recursion stack
            return false;
        }

        // Returns true if the graph contains a cycle, else false.
        boolean isCyclic() {
            // Mark all the vertices as not visited and not part of recursion stack
            boolean[] visited = new boolean[V];
            boolean[] recStack = new boolean[V];

            // Call the recursive helper function to detect cycle in different DFS trees
            for (int i = 0; i < V; i++)
                if (DFSUtil(i, visited, recStack))
                    return true;

            return false;
        }
    }

    private static boolean checkForUnboundedStructure(Molecule[] molecules) {
        int n = molecules.length;
        Graph graph = new Graph(n);

        // Build the graph
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j && molecules[i].canConnectWith(molecules[j])) {
                    graph.addEdge(i, j);
                }
            }
        }

        // Check for cycles in the graph
        return graph.isCyclic();
    }
}
