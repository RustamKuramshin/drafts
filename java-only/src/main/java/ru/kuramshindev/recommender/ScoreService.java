package ru.kuramshindev.recommender;

public class ScoreService implements Scorer<Document, User> {
    @Override
    public double getScore(Document doc, User user) {
        return 0;
    }
}
