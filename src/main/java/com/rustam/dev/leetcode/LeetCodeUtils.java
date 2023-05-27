package com.rustam.dev.leetcode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Super lightweight java library for leetcode!
 * A utility class that contains code that helps in solving problems from leetcode in the Java language.
 * Standard leetcode data structures augmented with useful methods.
 */
public class LeetCodeUtils {

    private LeetCodeUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * A standard class describing a singly linked list and complete with useful fields and methods.
     */
    public static class ListNode {

        public int val;
        public ListNode next;

        // Sign that a node is a tail in a looped linked list
        public boolean isTail;

        public ListNode() {
        }

        public ListNode(int x) {
            val = x;
            next = null;
        }

        public ListNode(int val, ListNode next) {
            this.val = val;
            this.next = next;
        }

        public ListNode(int x, boolean isTail) {
            this(x);
            this.isTail = isTail;
        }

        /**
         * Printing a singly linked list with circularity in mind
         */
        public void printListNode() {
            StringBuilder res = new StringBuilder();
            res.append("[");

            ListNode node = this;
            do {
                res.append(node.val);
                node = node.next;
                if (node != null) res.append(", ");
            } while (node != null && !node.isTail);

            if (node != null) res.append(node.val);

            res.append("]");

            System.out.println(res);
        }
    }

    /**
     * Standard class describing a doubly linked list and supplemented with useful fields and methods.
     */
    public static class Node {

        public Node prev;
        public Node next;

        public int val;

        // If you need to store the key in problems with caches
        public int key;

        public Node() {
        }

        public Node(int val) {
            this.val = val;
        }

        public Node(int key, int val) {
            this.key = key;
            this.val = val;
        }

        /**
         * Prints a linked list in the format: [1,2,3]
         */
        public void printNode() {

            StringBuilder res = new StringBuilder();
            res.append("[");

            Node node = this;
            do {
                res.append(node.val);
                node = node.next;
                if (node != null) res.append(", ");
            } while (node != null);

            res.append("]");

            System.out.println(res);
        }
    }

    /**
     * A standard class that describes a binary tree and is supplemented with useful fields and methods.
     */
    public static class TreeNode {

        public int val;
        public TreeNode left;
        public TreeNode right;

        public TreeNode() {
        }

        public TreeNode(int val) {
            this.val = val;
        }

        public TreeNode(int val, TreeNode left, TreeNode right) {
            this.val = val;
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TreeNode treeNode = (TreeNode) o;
            return val == treeNode.val && Objects.equals(left, treeNode.left) && Objects.equals(right, treeNode.right);
        }

        @Override
        public int hashCode() {
            return Objects.hash(val, left, right);
        }

        /**
         * Constructor that allows you to create a binary tree from a standard array representation: [2,1,3,null,4,null,7].
         *
         * @param treeAsStrArr
         * @return
         */
        public static TreeNode ofArrayString(String treeAsStrArr) {
            Integer[] arr = CommonUtils.arrayFromString(treeAsStrArr);
            return array2btree(arr, 0);
        }

        public int size() {
            return sizeRecursive(this);
        }

        private int sizeRecursive(TreeNode node) {
            if (node == null) {
                return 0;
            }

            int leftSize = sizeRecursive(node.left);
            int rightSize = sizeRecursive(node.right);

            return 1 + leftSize + rightSize;
        }

        //=============================================================================================================

        public enum TreeNodeMode {
            BINARY_SEARCH_TREE, SIMPLE_BINARY_TREE
        }

        public enum PredefinedNodePosition {
            HIGH, MIDDLE, LOW
        }

        public static RandomBinaryTreeBuilder randomBinaryTreeBuilder() {
            return new RandomBinaryTreeBuilder();
        }

        public static class RandomBinaryTreeBuilder {
            private int nodesCount;
            private int minNodeVal;
            private int maxNodeVal;
            private TreeNodeMode mode;

            public RandomBinaryTreeBuilder nodesCount(int nodesCount) {
                this.nodesCount = nodesCount;
                return this;
            }

            public RandomBinaryTreeBuilder minNodeVal(int minNodeVal) {
                this.minNodeVal = minNodeVal;
                return this;
            }

            public RandomBinaryTreeBuilder maxNodeVal(int maxNodeVal) {
                this.maxNodeVal = maxNodeVal;
                return this;
            }

            public RandomBinaryTreeBuilder mode(TreeNodeMode mode) {
                this.mode = mode;
                return this;
            }

            public TreeNode build() {
                return generateRandomBinaryTree(nodesCount, minNodeVal, maxNodeVal, mode);
            }
        }

        public static TreeNode generateRandomBinaryTree(int nodesCount, int minNodeVal, int maxNodeVal, TreeNodeMode mode) {
            if (nodesCount <= 0) {
                return null;
            }

            Random random = new Random();

            // Создаем корневой узел
            TreeNode root = new TreeNode(random.nextInt(maxNodeVal - minNodeVal + 1) + minNodeVal);

            // Определяем максимальное количество узлов, которые будут вставлены в дерево
            int maxInsertCount = nodesCount - 1;

            // Вставляем остальные узлы
            for (int i = 0; i < maxInsertCount; i++) {
                insertNode(root, new TreeNode(random.nextInt(maxNodeVal - minNodeVal + 1) + minNodeVal));
            }

            // Если указан режим BINARY_SEARCH_TREE, преобразуем дерево в двоичное дерево поиска
            if (mode == TreeNodeMode.BINARY_SEARCH_TREE) {
                convertToBinarySearchTree(root);
            }

            return root;
        }

        // Вспомогательный метод для вставки узла в дерево
        private static void insertNode(TreeNode root, TreeNode node) {
            if (root == null || node == null) {
                return;
            }

            Queue<TreeNode> queue = new LinkedList<>();
            queue.offer(root);

            while (!queue.isEmpty()) {
                TreeNode currNode = queue.poll();

                if (currNode.left == null) {
                    currNode.left = node;
                    break;
                } else {
                    queue.offer(currNode.left);
                }

                if (currNode.right == null) {
                    currNode.right = node;
                    break;
                } else {
                    queue.offer(currNode.right);
                }
            }
        }

        // Метод для преобразования дерева в двоичное дерево поиска
        private static void convertToBinarySearchTree(TreeNode root) {
            List<Integer> values = new ArrayList<>();
            inorderTraversal(root, values);
            Collections.sort(values); // Сортируем значения узлов
            rebuildTree(root, values);
        }

        // Обход дерева в порядке "inorder" и сохранение значений в список
        private static void inorderTraversal(TreeNode root, List<Integer> values) {
            if (root == null) {
                return;
            }

            inorderTraversal(root.left, values);
            values.add(root.val);
            inorderTraversal(root.right, values);
        }

        // Перестроение дерева с использованием отсортированных значений
        private static void rebuildTree(TreeNode root, List<Integer> values) {
            if (root == null || values.isEmpty()) {
                return;
            }

            rebuildTree(root.left, values);

            // Заменяем значения узлов на отсортированные
            root.val = values.remove(0);

            rebuildTree(root.right, values);
        }


        //=============================================================================================================

        public void printBinaryTree() {
            printBinaryTreeHelper(this, "", false);
        }

        private void printBinaryTreeHelper(TreeNode node, String prefix, boolean isLeft) {
            if (node == null) {
                return;
            }

            System.out.println(prefix + (isLeft ? "├── " : "└── ") + node.val);

            printBinaryTreeHelper(node.left, prefix + (isLeft ? "│   " : "    "), true);
            printBinaryTreeHelper(node.right, prefix + (isLeft ? "│   " : "    "), false);
        }

        //=============================================================================================================

        public void addNode(TreeNode node) {
            if (node == null) {
                return;
            }

            if (this.equals(node)) {
                return; // Узел уже присутствует в дереве, прекращаем вставку
            }

            if (node.val < this.val) {
                if (left != null) {
                    left.addNode(node); // Рекурсивно вставляем в левое поддерево
                } else {
                    left = node; // Вставляем узел как левого ребенка
                }
            } else if (node.val > this.val) {
                if (right != null) {
                    right.addNode(node); // Рекурсивно вставляем в правое поддерево
                } else {
                    right = node; // Вставляем узел как правого ребенка
                }
            }
        }

        /**
         * A method that converts an integer array obtained by breadth-first traversal of a binary tree into a binary tree.
         *
         * @param arr
         * @param i
         * @return
         */
        public static TreeNode array2btree(Integer[] arr, int i) {
            TreeNode treeNode = null;

            if (i < arr.length) {

                if (arr[i] == null) return null;

                treeNode = new TreeNode(arr[i]);

                treeNode.left = array2btree(arr, 2 * i + 1);

                treeNode.right = array2btree(arr, 2 * i + 2);
            }

            return treeNode;
        }

        /**
         * A method that converts a binary tree to an integer array obtained by breadth-first traversal of the binary tree.
         * @param root
         * @return
         */
        public static Integer[] btree2array(TreeNode root) {
            if (root == null) {
                return new Integer[0];
            }

            List<Integer> list = new ArrayList<>();
            Queue<TreeNode> queue = new LinkedList<>();

            queue.offer(root);

            while (!queue.isEmpty()) {
                int size = queue.size();
                for (int i = 0; i < size; i++) {
                    TreeNode node = queue.poll();
                    list.add(node.val);
                    if (node.left != null) {
                        queue.offer(node.left);
                    }
                    if (node.right != null) {
                        queue.offer(node.right);
                    }
                }
            }

            Integer[] result = new Integer[list.size()];
            for (int i = 0; i < list.size(); i++) {
                result[i] = list.get(i);
            }

            return result;
        }
    }

    /**
     * Test case player in the task with cache.
     */
    public static class CacheTestCasePlayer {

        /**
         * Method launching test cases.
         * SAVE THE TEST DATA IN FILES IN THE RESOURCES DIRECTORY!
         *
         * @param cacheClass       - Cache class reference (clazz for reflection).
         * @param methodsFilePath  - Path to the file where the array of methods is saved.
         * @param testDataFilePath - Path to the file where the array of test data is saved.
         * @throws NoSuchMethodException
         */
        public static void playTestCase(Class<?> cacheClass, String methodsFilePath, String testDataFilePath) {

            String methods = CommonUtils.readFileToString(methodsFilePath).replaceAll("\\s+","");
            String testData = CommonUtils.readFileToString(testDataFilePath).replaceAll("\\s+","");

            // processing methods
            List<String> methodsList = Arrays.stream(methods.substring(1, methods.length() - 1).split(","))
                    .map(m -> m.replaceAll("\\\"", ""))
                    .collect(Collectors.toList());

            // processing test data
            List<Integer[]> tdl = Arrays.stream(testData.substring(1, testData.length() - 1).split("],\\["))
                    .map(td -> {

                        String tdWithoutBrackets = td.replace("[", "").replace("]", "");

                        if (tdWithoutBrackets.matches("(\\d+),(\\d+)")) {
                            var tsa = tdWithoutBrackets.split(",");
                            return new Integer[]{Integer.parseInt(tsa[0]), Integer.parseInt(tsa[1])};
                        } else {
                            return new Integer[]{Integer.parseInt(tdWithoutBrackets)};
                        }

                    })
                    .collect(Collectors.toList());

            if (methodsList.size() != tdl.size()) {
                throw new IllegalStateException("The number of methods and data must match");
            }

            // play
            Constructor<?> constructor = null;
            try {
                constructor = cacheClass.getConstructor(int.class);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            Object cache = null;
            try {
                cache = constructor.newInstance(tdl.get(0)[0]);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }

            for (int i = 1; i < methodsList.size() - 1; i++) {

                try {
                    switch (methodsList.get(i)) {
                        case "put":
                            Method putMethod = cache.getClass().getDeclaredMethod("put", int.class, int.class);
                            putMethod.invoke(cache, tdl.get(i)[0], tdl.get(i)[1]);
                            break;
                        case "get":
                            Method getMethod = cache.getClass().getDeclaredMethod("get", int.class);
                            getMethod.invoke(cache, tdl.get(i)[0]);
                            break;
                        default:
                            throw new RuntimeException("Unknown method");
                    }
                } catch (Exception e) {
                    System.out.println("Tested code failed");
                    System.out.printf("method = %s%n", methodsList.get(i));
                    System.out.printf("test data = %s%n", Arrays.toString(tdl.get(i)));
                    throw new RuntimeException("Testing code call failed", e);
                }
            }
        }
    }

    /**
     * Common private helper methods are not relevant to leetcode problems.
     */
    public static class CommonUtils {

        /**
         * Reading a file into a String
         *
         * @param filePath - Path to file
         * @return - File content
         */
        public static String readFileToString(String filePath) {

            ClassLoader classLoader = LeetCodeUtils.CacheTestCasePlayer.class.getClassLoader();
            StringBuilder sb = new StringBuilder();

            try (InputStream inputStream = classLoader.getResourceAsStream(filePath);
                 InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(streamReader)) {

                String line;
                String ls = System.getProperty("line.separator");
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                    sb.append(ls);
                }

                sb.deleteCharAt(sb.length() - 1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return sb.toString();
        }

        /**
         * Gets an array of integers from its serialized representation.
         * Ex: [2,1,3,null,4,null,7] -> Integer[]
         *
         * @param strArr - String representation of an array
         * @return - integer array
         */
        public static Integer[] arrayFromString(String strArr) {

            List<Integer> integerList = new ArrayList<>();

            var a = strArr.substring(1, strArr.length() - 1).split(",");

            if (a.length == 1 && (a[0].equals("") || a[0].matches("\\s+"))) return new Integer[]{};

            for (String num : a) {
                if (num.equals("null")) {
                    integerList.add(null);
                } else {
                    integerList.add(Integer.parseInt(num.trim()));
                }
            }

            return integerList.toArray(new Integer[0]);
        }
    }
}