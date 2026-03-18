package com.rustam.dev.concurrency;

public class Deadlock {

    public static void main(String[] args) {

        final Friend alphonse = new Friend("Alphonse");

        final Friend gaston = new Friend("Gaston");

        Thread alphonseThread = new Thread(() -> alphonse.bow(gaston));
        alphonseThread.setName("ALPHONSE-THREAD");

        Thread gastonThread =new Thread(() -> gaston.bow(alphonse));
        gastonThread.setName("GASTON-THREAD");

        alphonseThread.start();
        gastonThread.start();
    }
}

class Friend {

    private final String name;

    public Friend(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public synchronized void bow(Friend bower) {
        System.out.format("%s: %s" + "  has bowed to me!%n", this.name, bower.getName());
        bower.bowBack(this);
    }

    public synchronized void bowBack(Friend bower) {
        System.out.format("%s: %s" + " has bowed back to me!%n", this.name, bower.getName());
    }
}
