package com.rustam.dev.leetcode;

import java.util.*;

public class TreeNodeUtils {

    // Ex: [2,1,3,null,4,null,7] -> Integer[]
    private static Integer[] arrayFromString(String strArr) {

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

    // Integer[] -> TreeNode root
    private static TreeNode insertLevelOrder(Integer[] arr, int i) {
        TreeNode treeNode = null;

        if (i < arr.length) {

            if (arr[i] == null) return null;

            treeNode = new TreeNode(arr[i]);

            treeNode.left = insertLevelOrder(arr, 2 * i + 1);

            treeNode.right = insertLevelOrder(arr, 2 * i + 2);
        }

        return treeNode;
    }

    public static TreeNode treeNodeFromStringArray(String treeAsStrArr) {

        Integer[] arr = arrayFromString(treeAsStrArr);

        var t = insertLevelOrder(arr, 0);

        return t;
    }

    public static void main(String[] args) {
        treeNodeFromStringArray("[1,3,2,5]");
        treeNodeFromStringArray("[2,1,3,null,4,null,7]");
    }
}
