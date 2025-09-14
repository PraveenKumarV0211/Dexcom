package com.example.demo.Model;

import lombok.Data;

@Data
public class GlucoseRangeCount {
    int low;
    int good;
    int high;
    int veryHigh;

    public GlucoseRangeCount(int low, int good, int high, int veryHigh) {
        this.low = low;
        this.good = good;
        this.high = high;
        this.veryHigh = veryHigh;
    }
}
