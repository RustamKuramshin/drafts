package ru.kuramshindev.algorithms.tree;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.kuramshindev.TreeAlgo;
import ru.kuramshindev.TreeNode;

class TreeAlgoTest {

    private final TreeAlgo treeAlgo = new TreeAlgo();

    @Test
    @DisplayName("maxDepth should return 0 for an empty tree")
    void maxDepth_returnsZeroForNullRoot() {
        assertThat(treeAlgo.maxDepth(null)).isZero();
    }

    @Test
    @DisplayName("maxDepth should return 1 for a single-node tree")
    void maxDepth_returnsOneForSingleNode() {
        TreeNode root = TreeNode.leaf(42);

        assertThat(treeAlgo.maxDepth(root)).isEqualTo(1);
    }

    @Test
    @DisplayName("maxDepth should follow the longest branch")
    void maxDepth_returnsDepthOfLongestBranch() {
        TreeNode root = TreeNode.of(
                1,
                TreeNode.of(
                        2,
                        TreeNode.leaf(4),
                        TreeNode.of(5, TreeNode.leaf(8), null)
                ),
                TreeNode.of(
                        3,
                        null,
                        TreeNode.of(
                                6,
                                null,
                                TreeNode.of(7, null, null)
                        )
                )
        );

        assertThat(treeAlgo.maxDepth(root)).isEqualTo(4);
    }
}
