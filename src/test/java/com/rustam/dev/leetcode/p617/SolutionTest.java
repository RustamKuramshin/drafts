package com.rustam.dev.leetcode.p617;

import com.rustam.dev.leetcode.TreeNode;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import static com.rustam.dev.leetcode.TreeNodeUtils.treeNodeFromStringArray;

@DisplayName("Тестирование метода слияния двух двоичных деревьев")
public class SolutionTest {

    @Test
    @DisplayName("Базовый пример Example 1")
    public void test_example_1() {

        TreeNode root1 = treeNodeFromStringArray("[1,3,2,5]");
        TreeNode root2 = treeNodeFromStringArray("[2,1,3,null,4,null,7]");

        Solution s = new Solution();
        TreeNode actual = s.mergeTrees(root1, root2);

        // expected: [3,4,5,5,4,null,7]
    }

    @Test
    @DisplayName("Базовый пример Example 2")
    public void test_example_2() {

        TreeNode root1 = treeNodeFromStringArray("[1]");
        TreeNode root2 = treeNodeFromStringArray("[1,2]");

        Solution s = new Solution();
        TreeNode actual = s.mergeTrees(root1, root2);

        // expected: [2,2]
    }
}
