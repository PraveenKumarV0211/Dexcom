package com.example.demo.Controller;

import com.example.demo.Model.KnowledgeDocument;
import com.example.demo.Service.KnowledgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
@CrossOrigin
public class KnowledgeController {

    @Autowired
    private KnowledgeService knowledgeService;

    @PostMapping
    public KnowledgeDocument add(@RequestBody Map<String, String> request) throws Exception {
        return knowledgeService.save(
                request.get("text"),
                request.get("category"),
                request.get("source")
        );
    }
}