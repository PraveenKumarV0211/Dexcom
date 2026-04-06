package com.example.demo.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Data
@Document(collection = "Dexcom_Data")
public class Glucose {
    @Indexed(name = "glucose_datetime_idx")
    @Field("DateTime")
    @JsonProperty("DateTime")
    Date DateTime;
    @JsonProperty("Glucose")
    Integer Glucose;

    public Glucose() {}
    public Glucose(Date DateTime, Integer Glucose) {
        this.DateTime = DateTime;
        this.Glucose = Glucose;
    }
}
