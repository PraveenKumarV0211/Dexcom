package com.example.demo.Repository;

import com.example.demo.Model.Glucose;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Repository
public interface GlucoseRepository extends MongoRepository<Glucose, String> {

    @Aggregation(pipeline = {"{ $group: { _id: null, averageGlucose: { $avg: '$Glucose' } } }"})
    Double findOverallAverageGlucose();

    @Aggregation(pipeline = {
            "{ $match: { 'timestamp': { $gte: ?0, $lt: ?1 } } }",
            "{ $group: { _id: null, average: { $avg: '$Glucose' } } }"
    })
    Double findAverageReadingValueForToday(Date localDateTime, Date localDateTime1);
}
