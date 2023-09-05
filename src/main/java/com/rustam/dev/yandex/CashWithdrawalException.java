package com.rustam.dev.yandex;

public class CashWithdrawalException extends Exception {

    public CashWithdrawalException() {
        super("Ошибка выдачи наличных");
    }
}
