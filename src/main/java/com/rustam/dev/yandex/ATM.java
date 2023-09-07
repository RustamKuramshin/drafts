package com.rustam.dev.yandex;

import java.util.*;

/**
 * Банкомат.
 * Инициализируется набором купюр и умеет выдавать купюры для заданной суммы, либо отвечать отказом.
 * При выдаче купюры списываются с баланса банкомата.
 * Допустимые номиналы: 50₽, 100₽, 500₽, 1000₽, 5000₽.
 */
public class ATM {

    private final TreeMap<Banknotes, Integer> store;

    public ATM(Map<Banknotes, Integer> banknotes) {
        this.store = new TreeMap<>(Comparator.comparing(b -> b.value));
        this.store.putAll(banknotes);
    }

    public Map<Banknotes, Integer> withdrawal(int sum) throws CashWithdrawalException {

        if (getBalanceValue() < sum) throw new CashWithdrawalException();

        int sumVal = sum;

        Map<Banknotes, Integer> res = new HashMap<>();

        for (Map.Entry<Banknotes, Integer> entry : store.descendingMap().entrySet()) {
            Banknotes k = entry.getKey();
            Integer v = entry.getValue();

            if (sumVal == 0) break;

            if (v == 0) continue;

            int bCount = Math.min(sumVal/k.value, v);

            res.put(k, bCount);
            store.put(k, store.get(k) - bCount);

            sumVal = sumVal - k.value * bCount;
        }

        return res;
    }

    private int getBalanceValue() {
        int result = 0;

        for(Map.Entry<Banknotes, Integer> entry : store.entrySet()) {
            result += entry.getKey().value * entry.getValue();
        }

        return result;
    }

    public static void main(String[] args) throws CashWithdrawalException {
        // 500 - 1
        // 100 - 2
        // 50 - 3
        // total - 850
        var atm = new ATM(Map.of(Banknotes.R500, 1, Banknotes.R100, 2, Banknotes.R50, 3));
        System.out.println(atm.withdrawal(900));
    }
}

enum Banknotes {
    R50(50), R100(100), R500(500), R1000(1000), R5000(5000);

    public final int value;

    Banknotes(int value) {
        this.value = value;
    }
}

class CashWithdrawalException extends Exception {

    public CashWithdrawalException() {
        super("Ошибка выдачи наличных");
    }
}
