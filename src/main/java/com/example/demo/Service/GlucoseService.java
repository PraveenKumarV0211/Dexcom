package com.example.demo.Service;

import com.example.demo.Model.Glucose;
import com.example.demo.Repository.GlucoseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class GlucoseService {

    @Autowired
    GlucoseRepository repository;

    public List<Glucose> getReadings(){
        return repository.findAll();
    }

}
