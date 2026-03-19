package ru.kuramshindev.leetcode.p098;

import ru.kuramshindev.leetcode.treenode.p098.Solution;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import static ru.kuramshindev.leetcode.LeetCodeUtils.TreeNode;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Тестирование метода валидации структуры двоичного дерева поиска")
public class SolutionTest {

    @Test
    @DisplayName("Базовый пример Example 1")
    public void test_example_1() {
        //arrange
        TreeNode root = TreeNode.ofArrayString("[2,1,3]");

        //act
        Solution s = new Solution();
        boolean actual = s.isValidBST(root);

        //assert
        assertTrue(actual);
    }

    @Test
    @DisplayName("Базовый пример Example 2")
    public void test_example_2() {
        //arrange
        TreeNode root = TreeNode.ofArrayString("[5,1,4,null,null,3,6]");

        //act
        Solution s = new Solution();
        boolean actual = s.isValidBST(root);

        //assert
        assertFalse(actual);

    }
}
