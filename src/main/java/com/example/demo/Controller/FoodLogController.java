package com.example.demo.Controller;

import com.example.demo.Model.FoodLog;
import com.example.demo.Service.FoodLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/food-logs")
@CrossOrigin
public class FoodLogController {

    @Autowired
    private FoodLogService foodLogService;

    @PostMapping
    public FoodLog create(@RequestBody FoodLog foodLog) {
        return foodLogService.save(foodLog);
    }

    @GetMapping
    public List<FoodLog> getAll() {
        return foodLogService.getAll();
    }

    @GetMapping("/range")
    public List<FoodLog> getByRange(
            @RequestParam LocalDateTime from,
            @RequestParam LocalDateTime to) {
        return foodLogService.getByDateRange(from, to);
    }

    @GetMapping("/search")
    public List<FoodLog> searchByFood(@RequestParam String food) {
        return foodLogService.searchByFood(food);
    }

    @PutMapping("/{id}")
    public FoodLog update(@PathVariable String id, @RequestBody FoodLog foodLog) {
        return foodLogService.update(id, foodLog);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        foodLogService.delete(id);
    }
}