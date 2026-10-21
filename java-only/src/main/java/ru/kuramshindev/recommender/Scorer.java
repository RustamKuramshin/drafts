package ru.kuramshindev.recommender;

public interface Scorer<Document, User> {
    double getScore(Document doc, User user);
}
