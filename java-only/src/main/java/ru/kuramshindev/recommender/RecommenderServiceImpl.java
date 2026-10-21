package ru.kuramshindev.recommender;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RecommenderServiceImpl implements RecommenderService<Document, User> {

    private final Scorer scoreService;

    private final DocumentRepository documentRepository;


    public RecommenderServiceImpl() {
        documentRepository = new DocumentRepository();
        scoreService = new ScoreService();
    }

    @Override
    public List<Document> getTop(User user, int limit) {

        var result = new ArrayList<Document>();

        var scoreMap = new TreeMap<Double, List<Document>>(Comparator.reverseOrder());

        var docs = documentRepository.getAll();

        for (int i = 0; i < docs.size(); i++) {
            var currentDoc = docs.get(i);
            var docScore = scoreService.getScore(currentDoc, user);
            if (scoreMap.containsKey(docScore)) {
                var currentDos = scoreMap.get(docScore);
                currentDos.add(currentDoc);
            } else {
                List<Document> newDocs = new ArrayList<>();
                newDocs.add(currentDoc);
                scoreMap.put(docScore, newDocs);
            }
        }

        // scoreMap / limit = 3
        // 10.0 -> [a,b,c,d]
        // 8.5 -> [e,f]
        // 4.3 -> [r, y]

        int counter = 0;

        outer:
        for (Map.Entry<Double, List<Document>> entry : scoreMap.entrySet()) {
            if (counter < limit) {
                for (int j = 0; j < entry.getValue().size(); j++) {
                    var currDocs = entry.getValue();
                    result.add(currDocs.get(j));

                    if (j + 1 > result.size()) {
                        break outer;
                    }
                }
            }
        }

        return result;
    }

    @Override
    public void addDocument(Document document) {
        documentRepository.put(document);
    }
}
