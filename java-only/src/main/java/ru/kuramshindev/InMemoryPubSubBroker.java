package ru.kuramshindev;

import java.util.function.Consumer;

//Реализуйте простой in-memory pub/sub брокер сообщений.
//
//Брокер должен позволять:
//
//1. Подписываться на топик.
//2. Публиковать сообщение в топик.
//3. Доставлять сообщение всем активным подписчикам этого топика.
//4. Отписываться от топика через Subscription.close().
//5. Гарантировать порядок доставки сообщений одному подписчику в рамках одного топика.
//6. Не доставлять сообщения подписчикам других топиков.
//Решение должно быть in-memory
//Потокобезопасность можно считать дополнительным плюсом.
public class InMemoryPubSubBroker implements PubSubBroker {

    @Override
    public Subscription subscribe(String topic, Consumer<String> handler) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void publish(String topic, String message) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
