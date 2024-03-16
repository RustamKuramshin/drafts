package com.rustam.dev.dsa.trees;

class RedBlackTree {
    private static final boolean RED = true;
    private static final boolean BLACK = false;

    private class Node {
        int data;
        boolean color;
        Node left, right, parent;

        Node(int data) {
            this.data = data;
            this.color = RED; // Новый узел по умолчанию красный
            this.left = null;
            this.right = null;
            this.parent = null;
        }
    }

    private Node root;

    // Конструктор
    public RedBlackTree() {
        root = null;
    }

    // Вспомогательный метод для выполнения левого вращения
    private void leftRotate(Node x) {
        Node y = x.right;
        x.right = y.left;
        if (y.left != null) {
            y.left.parent = x;
        }
        y.parent = x.parent;
        if (x.parent == null) {
            this.root = y;
        } else if (x == x.parent.left) {
            x.parent.left = y;
        } else {
            x.parent.right = y;
        }
        y.left = x;
        x.parent = y;
    }

    // Вспомогательный метод для выполнения правого вращения
    private void rightRotate(Node x) {
        Node y = x.left;
        x.left = y.right;
        if (y.right != null) {
            y.right.parent = x;
        }
        y.parent = x.parent;
        if (x.parent == null) {
            this.root = y;
        } else if (x == x.parent.right) {
            x.parent.right = y;
        } else {
            x.parent.left = y;
        }
        y.right = x;
        x.parent = y;
    }

    // Вспомогательный метод для исправления нарушений после вставки
    private void fixInsert(Node k) {
        Node u;
        while (k.parent.color == RED) {
            if (k.parent == k.parent.parent.right) {
                u = k.parent.parent.left; // дядя
                if (u.color == RED) {
                    u.color = BLACK;
                    k.parent.color = BLACK;
                    k.parent.parent.color = RED;
                    k = k.parent.parent;
                } else {
                    if (k == k.parent.left) {
                        k = k.parent;
                        rightRotate(k);
                    }
                    k.parent.color = BLACK;
                    k.parent.parent.color = RED;
                    leftRotate(k.parent.parent);
                }
            } else {
                u = k.parent.parent.right; // дядя

                if (u.color == RED) {
                    u.color = BLACK;
                    k.parent.color = BLACK;
                    k.parent.parent.color = RED;
                    k = k.parent.parent;
                } else {
                    if (k == k.parent.right) {
                        k = k.parent;
                        leftRotate(k);
                    }
                    k.parent.color = BLACK;
                    k.parent.parent.color = RED;
                    rightRotate(k.parent.parent);
                }
            }
            if (k == root) {
                break;
            }
        }
        root.color = BLACK;
    }

    // Метод для вставки узла в красно-чёрное дерево
    public void insert(int data) {
        Node node = new Node(data);
        root = insertRec(root, node);
        fixInsert(node);
    }

    private Node insertRec(Node root, Node node) {
        if (root == null) {
            return node;
        }

        if (node.data < root.data) {
            root.left = insertRec(root.left, node);
            root.left.parent = root;
        } else if (node.data > root.data) {
            root.right = insertRec(root.right, node);
            root.right.parent = root;
        }

        return root;
    }

    // Вспомогательный метод для вывода дерева (для тестирования)
    public void printTree() {
        printHelper(this.root, "", true);
    }

    private void printHelper(Node root, String indent, boolean last) {
        if (root != null) {
            System.out.print(indent);
            if (last) {
                System.out.print("R----");
                indent += "   ";
            } else {
                System.out.print("L----");
                indent += "|  ";
            }
            String sColor = root.color == RED ? "RED" : "BLACK";
            System.out.println(root.data + "(" + sColor + ")");
            printHelper(root.left, indent, false);
            printHelper(root.right, indent, true);
        }
    }

    // Пример использования
    public static void main(String[] args) {
        RedBlackTree tree = new RedBlackTree();

        tree.insert(7);
        tree.insert(3);
        tree.insert(18);
        tree.insert(10);
        tree.insert(22);
        tree.insert(8);
        tree.insert(11);
        tree.insert(26);

        tree.printTree();
    }
}
