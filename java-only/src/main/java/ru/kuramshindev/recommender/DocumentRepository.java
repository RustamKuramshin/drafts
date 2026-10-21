package ru.kuramshindev.recommender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DocumentRepository {
    private final Map<Document, Object> docStore;

    public DocumentRepository() {
        docStore = new ConcurrentHashMap<>();
    }

    public Object put(Document document) {
       return docStore.put(document, null);
    }

    public List<Document> getAll() {
        return new ArrayList<>(docStore.keySet());
    }
}
