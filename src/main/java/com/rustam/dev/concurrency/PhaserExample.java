package com.rustam.dev.concurrency;

import java.util.concurrent.Phaser;

public class PhaserExample {

    public static void main(String[] args) {
        Phaser phaser = new Phaser(1); // Регистрация основного потока

        // Запуск трех потоков
        for (int i = 0; i < 3; i++) {
            phaser.register();
            new Thread(new Task(phaser), "Thread " + i).start();
        }

        // Дожидаемся завершения первой фазы всех потоков
        phaser.arriveAndAwaitAdvance();
        System.out.println("All threads have completed phase 1!");

        // Дожидаемся завершения второй фазы всех потоков
        phaser.arriveAndAwaitAdvance();
        System.out.println("All threads have completed phase 2!");

        // Дерегистрация основного потока и завершение работы phaser
        phaser.arriveAndDeregister();
    }

    static class Task implements Runnable {
        private final Phaser phaser;

        Task(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            System.out.println(Thread.currentThread().getName() + " completed phase 1");
            phaser.arriveAndAwaitAdvance();

            System.out.println(Thread.currentThread().getName() + " completed phase 2");
            phaser.arriveAndDeregister();
        }
    }
}
