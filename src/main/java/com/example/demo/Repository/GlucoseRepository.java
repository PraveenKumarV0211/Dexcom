package com.example.demo.Repository;

import com.example.demo.Model.Glucose;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlucoseRepository extends MongoRepository<Glucose, String> {

}
