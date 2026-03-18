package ru.kuramshindev;

public class Main {

    static void main(String[] args) {
        FactorialCalculator factorialCalculator = new FactorialCalculator();
        TreeAlgo treeAlgo = new TreeAlgo();
        TreeNode tree = TreeNode.of(
                10,
                TreeNode.leaf(5),
                TreeNode.of(20, TreeNode.leaf(15), null)
        );

        System.out.println("6! = " + factorialCalculator.calculate(6));
        System.out.println("Tree max depth = " + treeAlgo.maxDepth(tree));
    }
}
