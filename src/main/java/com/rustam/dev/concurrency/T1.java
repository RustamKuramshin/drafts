package com.rustam.dev.concurrency;

public class T1 {

    private static final int lc = 100;

    private int count = 0;

    private void inc() {
        ++count;
    }

    private void dec() {
        --count;
    }

    private int getCount() {
        return this.count;
    }

    public static void main(String[] args) throws InterruptedException {

        for (int j = 0; j < 100; j++) {

            T1 t1 = new T1();

            for (int i = 0; i < lc; i++) {
                int finalI = i;
                var t = new Thread(() -> {
                    if (finalI%2 == 0) {
                        t1.inc();
                    } else {
                        t1.dec();
                    }
//                    System.out.printf("Hello from %s%n", Thread.currentThread().getName());
                });
                t.start();
            }

            Thread.sleep(1000);

            var c = t1.getCount();

            System.out.printf("End with: %s%n", c);

            try {
                assert c == 0;
            } catch (AssertionError e) {
                System.out.println(e);
            }
        }
    }
}
