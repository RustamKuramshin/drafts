package com.rustam.dev.trees;

import java.util.LinkedList;
import java.util.Queue;

class TreeNode {
    int value;
    TreeNode left, right;

    TreeNode(int value) {
        this.value = value;
        left = null;
        right = null;
    }
}

public class BinaryTree {
    TreeNode root;

    // Прямой обход
    void printPreOrder(TreeNode node) {
        if (node == null) {
            return;
        }
        System.out.print(node.value + " ");
        printPreOrder(node.left);
        printPreOrder(node.right);
    }

    // Центрированный обход
    void printInOrder(TreeNode node) {
        if (node == null) {
            return;
        }
        printInOrder(node.left);
        System.out.print(node.value + " ");
        printInOrder(node.right);
    }

    // Обратный обход
    void printPostOrder(TreeNode node) {
        if (node == null) {
            return;
        }
        printPostOrder(node.left);
        printPostOrder(node.right);
        System.out.print(node.value + " ");
    }

    // Функция для обхода в ширину
    void printLevelOrder() {
        Queue<TreeNode> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            TreeNode tempNode = queue.poll();
            System.out.print(tempNode.value + " ");

            // Добавление левого потомка в очередь
            if (tempNode.left != null) {
                queue.add(tempNode.left);
            }

            // Добавление правого потомка в очередь
            if (tempNode.right != null) {
                queue.add(tempNode.right);
            }
        }
    }

    // Рекурсивная функция для вставки нового ключа в BST
    TreeNode insertRec(TreeNode root, int value) {
        // Если дерево пусто, возвращаем новый узел
        if (root == null) {
            root = new TreeNode(value);
            return root;
        }

        // Иначе, рекурсивно спускаемся вниз по дереву
        if (value < root.value)
            root.left = insertRec(root.left, value);
        else if (value > root.value)
            root.right = insertRec(root.right, value);

        // Возвращаем указатель на (неизменённый) узел root
        return root;
    }

    // Метод для вызова рекурсивной функции вставки
    void insert(int value) {
        root = insertRec(root, value);
    }

    // Метод для удаления ключа
    void delete(int value) {
        root = deleteRec(root, value);
    }

    // Рекурсивная функция для удаления ключа
    TreeNode deleteRec(TreeNode root, int value) {
        if (root == null) return root;

        // Рекурсивный поиск узла для удаления
        if (value < root.value)
            root.left = deleteRec(root.left, value);
        else if (value > root.value)
            root.right = deleteRec(root.right, value);
        else {
            // Узел с одним потомком или без потомков
            if (root.left == null)
                return root.right;
            else if (root.right == null)
                return root.left;

            // Узел с двумя потомками: Получаем преемника (самый маленький в правом поддереве)
            root.value = minValue(root.right);

            // Удаление преемника
            root.right = deleteRec(root.right, root.value);
        }

        return root;
    }

    int minValue(TreeNode root) {
        int minv = root.value;
        while (root.left != null) {
            minv = root.left.value;
            root = root.left;
        }
        return minv;
    }


    public static void main(String[] args) {
        BinaryTree tree = new BinaryTree();
        tree.root = new TreeNode(1);
        tree.root.left = new TreeNode(2);
        tree.root.right = new TreeNode(3);
        tree.root.left.left = new TreeNode(4);
        tree.root.left.right = new TreeNode(5);

        System.out.println("Прямой обход:");
        tree.printPreOrder(tree.root);

        System.out.println("\nЦентрированный обход:");
        tree.printInOrder(tree.root);

        System.out.println("\nОбратный обход:");
        tree.printPostOrder(tree.root);
    }
}