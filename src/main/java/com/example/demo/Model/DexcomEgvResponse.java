package com.example.demo.Model;

import lombok.Data;
import java.util.List;

@Data
public class DexcomEgvResponse {
    private String recordType;
    private String recordVersion;
    private String userId;
    private List<DexcomEgvRecord> records;
}