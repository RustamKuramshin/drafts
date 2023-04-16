package com.rustam.dev.tinkoff.solutioncup;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

class CalculateSum {
    public static void main(String args[]) {
        BufferedReader inputDataBR = new BufferedReader(new InputStreamReader(System.in));

        List<Double> persents = List.of(0.1, 0.02);
        List<Double> purchases = List.of(100.0, 300.0);

        List<BigDecimal> persentsBD = persents.stream()
                .map(BigDecimal::new)
                .map(p -> p.setScale(2, RoundingMode.HALF_DOWN))
                .collect(Collectors.toList());

        List<BigDecimal> purchasesBD = purchases.stream()
                .map(BigDecimal::new)
                .map(p -> p.setScale(2, RoundingMode.HALF_DOWN))
                .collect(Collectors.toList());


        BigDecimal sum = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_DOWN);

        for (int i = 0; i < purchasesBD.size(); i++) {
            BigDecimal mul = purchasesBD.get(i).multiply(persentsBD.get(i)).setScale(2, RoundingMode.HALF_DOWN);
            sum = sum.add(mul).setScale(2, RoundingMode.HALF_DOWN);
        }

        System.out.println(sum);
    }
}
