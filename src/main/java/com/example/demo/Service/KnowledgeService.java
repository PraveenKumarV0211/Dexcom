package com.example.demo.Service;

import com.example.demo.Model.KnowledgeDocument;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private EmbeddingService embeddingService;

    public KnowledgeDocument save(String text, String category, String source) throws Exception {
        List<Double> embedding = embeddingService.getEmbedding(text);

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setText(text);
        doc.setCategory(category);
        doc.setSource(source);
        doc.setEmbedding(embedding);
        doc.setCreatedAt(LocalDateTime.now());

        return mongoTemplate.save(doc);
    }

    public List<String> search(String query, int limit) throws Exception {
        List<Double> queryVector = embeddingService.getEmbedding(query);

        Document vectorSearch = new Document("$vectorSearch",
                new Document()
                        .append("index", "vector_index")
                        .append("path", "embedding")
                        .append("queryVector", queryVector)
                        .append("numCandidates", 50)
                        .append("limit", limit)
        );

        Document project = new Document("$project",
                new Document()
                        .append("text", 1)
                        .append("score", new Document("$meta", "vectorSearchScore"))
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(
                Aggregation.newAggregation(
                        new CustomAggregationOperation(vectorSearch),
                        new CustomAggregationOperation(project)
                ),
                "knowledge_base",
                Document.class
        );

        return results.getMappedResults().stream()
                .map(doc -> doc.getString("text"))
                .collect(Collectors.toList());
    }

    private static class CustomAggregationOperation implements AggregationOperation {
        private final Document document;

        CustomAggregationOperation(Document document) {
            this.document = document;
        }

        @Override
        public Document toDocument(AggregationOperationContext context) {
            return document;
        }
    }
}