package com.rustam.dev.datsteam;

import lombok.Data;

enum NotificationType {
    SMS, EMAIL
}

@Data
class User {
    String name;
    NotificationType type;
}

interface NotificationService {
    void doNotify(User user);
}

// @Service
class SMSNotificationService implements NotificationService {

    @Override
    public void doNotify(User user) {
        // ...
    }
}

// @Service
class EmailNotificationService implements NotificationService{

    @Override
    public void doNotify(User user) {
        // ...
    }
}

/**
 * Дано Spring Boot приложение.
 * Есть пользователи User.
 * Есть сервисы SMS и EMAIL нотификации пользователей.
 * Виды способов нотификации пользователей будут постоянно увеличиваться!!!
 * logic() должен уведомлять пользователя в зависимости от типа уведомлений,
 * который для него назначен (user.getType()).
 * Нужно реализовать такой подход, который при увеличении кол-ва видов уведомлений НЕ потребует изменений
 * класса NotificationApplication!
 */
// @Service
public class NotificationApplication {

    void logic(User user) {

        // .doNotify(user)
    }
}
