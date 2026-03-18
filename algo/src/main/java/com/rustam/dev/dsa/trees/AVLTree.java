package com.rustam.dev.dsa.trees;

class Node {
    int key, height;
    Node left, right;

    Node(int d) {
        key = d;
        height = 1;
    }
}

public class AVLTree {
    Node root;

    // Получение высоты узла
    int height(Node N) {
        if (N == null)
            return 0;
        return N.height;
    }

    // Вспомогательная функция для правого вращения поддерева с корнем y
    Node rightRotate(Node y) {
        Node x = y.left;
        Node T2 = x.right;

        // Выполнение вращения
        x.right = y;
        y.left = T2;

        // Обновление высот
        y.height = Math.max(height(y.left), height(y.right)) + 1;
        x.height = Math.max(height(x.left), height(x.right)) + 1;

        // Возвращаем новый корень
        return x;
    }

    // Вспомогательная функция для левого вращения поддерева с корнем x
    Node leftRotate(Node x) {
        Node y = x.right;
        Node T2 = y.left;

        // Выполнение вращения
        y.left = x;
        x.right = T2;

        // Обновление высот
        x.height = Math.max(height(x.left), height(x.right)) + 1;
        y.height = Math.max(height(y.left), height(y.right)) + 1;

        // Возвращаем новый корень
        return y;
    }

    // Получение баланса узла N
    int getBalance(Node N) {
        if (N == null)
            return 0;
        return height(N.left) - height(N.right);
    }

    Node insert(Node node, int key) {
        // 1. Выполнение стандартной вставки BST
        if (node == null)
            return (new Node(key));

        if (key < node.key)
            node.left = insert(node.left, key);
        else if (key > node.key)
            node.right = insert(node.right, key);
        else // Дубликаты не допускаются
            return node;

        // 2. Обновление высоты этого предка
        node.height = 1 + Math.max(height(node.left), height(node.right));

        // 3. Получение баланса предка
        int balance = getBalance(node);

        // Если узел стал несбалансированным, то есть 4 случая

        // Left Left Case
        if (balance > 1 && key < node.left.key)
            return rightRotate(node);

        // Right Right Case
        if (balance < -1 && key > node.right.key)
            return leftRotate(node);

        // Left Right Case
        if (balance > 1 && key > node.left.key) {
            node.left = leftRotate(node.left);
            return rightRotate(node);
        }

        // Right Left Case
        if (balance < -1 && key < node.right.key) {
            node.right = rightRotate(node.right);
            return leftRotate(node);
        }

        // Возвращаем неизменённый узел
        return node;
    }

    // Функция для вставки ключа в дерево. Она возвращает корень нового дерева
    void insert(int key) {
        root = insert(root, key);
    }

    // Функция для вывода дерева (для удобства тестирования)
    void preOrder(Node node) {
        if (node != null) {
            System.out.print(node.key + " ");
            preOrder(node.left);
            preOrder(node.right);
        }
    }

    public static void main(String[] args) {
        AVLTree tree = new AVLTree();

        tree.insert(10);
        tree.insert(20);
        tree.insert(30);
        tree.insert(40);
        tree.insert(50);
        tree.insert(25);

        System.out.println("Префиксный обход сформированного AVL-дерева:");
        tree.preOrder(tree.root);
    }
}
