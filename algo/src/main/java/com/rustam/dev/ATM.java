package com.rustam.dev;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Реализовать класс Банкомата.
 * Банкомат инициализируется набором купюр и умеет выдавать купюры для заданной суммы, либо отвечать отказом.
 * При выдаче купюры списываются с баланса банкомата. Допустимые номиналы: 50₽, 100₽, 500₽, 1000₽, 5000₽.
 * В классе нужно реализовать метод "withdrawal", который принимает сумму к выдаче и возвращает выданные купюры.
 */
public class ATM {

    private TreeMap<Integer, Integer> bills = new TreeMap<>(Comparator.reverseOrder()); // номинал -> количество

    public ATM(Map<Integer, Integer> initialBills) {
        this.bills.putAll(initialBills);
    }

    public Map<Integer, Integer> withdrawal(int amount) {
        Map<Integer, Integer> result = new HashMap<>();

        for (int billValue : bills.keySet()) {
            int requiredBills = amount / billValue; // сколько купюр нужно данного номинала
            int availableBills = bills.get(billValue); // сколько купюр доступно
            int billsToGive = Math.min(requiredBills, availableBills); // сколько купюр выдадим

            if (billsToGive > 0) {
                result.put(billValue, billsToGive);
                bills.put(billValue, availableBills - billsToGive);
                amount -= billValue * billsToGive;
            }
        }

        if (amount > 0) {
            // Невозможно выдать запрошенную сумму доступными купюрами
            return null;
        }

        return result;
    }

    public static void main(String[] args) {
        Map<Integer, Integer> initialBills = new HashMap<>();
        initialBills.put(50, 10);
        initialBills.put(100, 5);
        initialBills.put(500, 5);
        initialBills.put(1000, 5);
        initialBills.put(5000, 5);

        ATM atm = new ATM(initialBills);
        System.out.println(atm.withdrawal(3650));
    }
}