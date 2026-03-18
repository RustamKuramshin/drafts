package ru.kuramshindev;

public record TreeNode(int value, TreeNode left, TreeNode right) {

    public static TreeNode leaf(int value) {
        return new TreeNode(value, null, null);
    }

    public static TreeNode of(int value, TreeNode left, TreeNode right) {
        return new TreeNode(value, left, right);
    }
}
