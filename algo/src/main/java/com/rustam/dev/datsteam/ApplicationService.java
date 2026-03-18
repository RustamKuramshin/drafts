package com.rustam.dev.datsteam;

import java.util.HashMap;

/**
 * Задача на code review.
 * Приведена попытка реализовать in-memory кэш.
 * Код будет выполняться в многопоточной среде.
 * Найти проблемы в коде и исправить.
 */
public class ApplicationService {

    public static volatile HashMap<Object, Object> map = new HashMap<Object, Object>();

    // ...
}
