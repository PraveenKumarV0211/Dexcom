package com.example.demo.Model;

import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@Document(collection = "Health_Events")
@CompoundIndex(name = "type_date_unique", def = "{'type': 1, 'date': 1}", unique = true)
public class HealthEvent {
    private String type;
    private String units;
    private String date;
    private Map<String, Object> data;

    public HealthEvent() {}
}