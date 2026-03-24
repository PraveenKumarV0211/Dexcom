package com.example.demo.Controller;

import com.example.demo.Service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping
    public Map<String, String> chat(@RequestBody Map<String, Object> request) {
        String question = (String) request.get("question");
        List<Map<String, String>> history = (List<Map<String, String>>) request.getOrDefault("history", List.of());
        String answer = chatService.chat(question, history);
        return Map.of("answer", answer);
    }
}