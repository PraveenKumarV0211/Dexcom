package com.example.demo.Service;

import com.example.demo.Model.FoodLog;
import com.example.demo.Repository.FoodLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FoodLogService {

    @Autowired
    private FoodLogRepository foodLogRepo;

    public FoodLog save(FoodLog foodLog) {
        if (foodLog.getTimestamp() == null) {
            foodLog.setTimestamp(LocalDateTime.now());
        }
        return foodLogRepo.save(foodLog);
    }

    public List<FoodLog> getAll() {
        return foodLogRepo.findAllByOrderByTimestampDesc();
    }

    public List<FoodLog> getByDateRange(LocalDateTime from, LocalDateTime to) {
        return foodLogRepo.findByTimestampBetween(from, to);
    }

    public List<FoodLog> searchByFood(String foodName) {
        return foodLogRepo.findByFoodNameRegexIgnoreCaseOrderByTimestampDesc(foodName);
    }

    public List<FoodLog> getLastNByFood(String foodName) {
        return foodLogRepo.findTop3ByFoodNameRegexIgnoreCaseOrderByTimestampDesc(foodName);
    }

    public FoodLog update(String id, FoodLog updated) {
        FoodLog existing = foodLogRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Food log not found: " + id));

        existing.setFoodName(updated.getFoodName());
        existing.setMealType(updated.getMealType());
        existing.setPortionSize(updated.getPortionSize());
        existing.setSource(updated.getSource());
        existing.setNotes(updated.getNotes());
        existing.setTimestamp(updated.getTimestamp());
        existing.setCarbs(updated.getCarbs());
        existing.setProtein(updated.getProtein());
        existing.setFiber(updated.getFiber());

        return foodLogRepo.save(existing);
    }

    public void delete(String id) {
        foodLogRepo.deleteById(id);
    }
}
