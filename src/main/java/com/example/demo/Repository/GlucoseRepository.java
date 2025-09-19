package com.example.demo.Repository;

import com.example.demo.Model.Glucose;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface GlucoseRepository extends MongoRepository<Glucose, String> {

    @Aggregation(pipeline = {"{ $group: { _id: null, averageGlucose: { $avg: '$Glucose' } } }"})
    Double findOverallAverageGlucose();

    @Aggregation(pipeline = {
            "{ $match: { 'DateTime': { $gte: ?0, $lt: ?1 } } }",
            "{ $group: { _id: null, average: { $avg: '$Glucose' } } }"
    })
    Double findAverageReadingValueForToday(Date start, Date end);

    @Aggregation(pipeline = {
            "{ $group: { _id: null, stdDev: { $stdDevPop: '$Glucose' } } }"
    })
    Double findStandardDeviation();

    List<Glucose> findByDateTimeBetween(Date startTime, Date currentTime);

    @Query("{ 'DateTime': { $gte: ?0, $lte: ?1 } }")
    Page<Glucose> findByDateTimeBetween(Date startDate, Date endDate, Pageable pageable);
}
