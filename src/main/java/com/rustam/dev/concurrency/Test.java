package com.rustam.dev.concurrency;

public class Test implements Runnable {

    private Integer num = 0;

    @Override
    public void run() {

        while (true) {
            System.out.println("Hello");
            this.num = this.num + 1;
        }
//        try {
//            Thread.sleep(120_000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

    }

    public static void main(String[] args) {
        var t = new Test();
        while (true) new Thread(t).start();
    }
}
