package com.rustam.dev;

public class AnimatedChristmasTree {

    public static void main(String[] args) throws InterruptedException {
        int height = 7;
        int numFrames = 20;
        int maxRays = 12;
        int delay = 200;

        for (int frame = 0; frame < numFrames; frame++) {
            clearScreen();
            drawTree(height, frame, maxRays);
            Thread.sleep(delay);
        }
    }

    private static void drawTree(int height, int frame, int maxRays) {

        for (int i = 1; i <= height; i++) {
            for (int j = i; j < height; j++) {
                System.out.print(" ");
            }

            for (int k = 1; k <= (2 * i - 1); k++) {
                System.out.print("*");
            }
            System.out.println();
        }

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < height - 1; j++) {
                System.out.print(" ");
            }
            System.out.println("*");
        }

        int numRays = (frame % (maxRays + 1));
        for (int r = 0; r < numRays; r++) {
            int angle = (r * 360 / maxRays) + frame * 10;
            drawRay(height, angle);
        }
    }

    private static void drawRay(int treeHeight, int angle) {
        int x = (int) (treeHeight + Math.cos(Math.toRadians(angle)) * treeHeight);
        int y = (int) (Math.sin(Math.toRadians(angle)) * treeHeight);

        for (int i = 0; i < y; i++) {
            System.out.print(" ");
        }

        System.out.println("*");
    }

    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();

        for (int i = 0; i < 50; i++) {
            System.out.println();
        }
    }
}

