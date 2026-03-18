package com.example.demo.Repository;

import com.example.demo.Model.FoodLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FoodLogRepository extends MongoRepository<FoodLog, String> {

    List<FoodLog> findByTimestampBetween(LocalDateTime from, LocalDateTime to);

    List<FoodLog> findByFoodNameRegexIgnoreCaseOrderByTimestampDesc(String foodName);

    List<FoodLog> findTop3ByFoodNameRegexIgnoreCaseOrderByTimestampDesc(String foodName);

    List<FoodLog> findAllByOrderByTimestampDesc();
}