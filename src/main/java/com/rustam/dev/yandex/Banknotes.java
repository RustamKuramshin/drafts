package com.rustam.dev.yandex;

public enum Banknotes {
    R50(50), R100(100), R500(500), R1000(1000), R5000(5000);

    public int value;

    Banknotes(int value) {
        this.value = value;
    }
}
