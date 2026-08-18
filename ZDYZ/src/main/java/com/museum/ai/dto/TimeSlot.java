package com.museum.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlot {
    private String timeMark;
    private String startTime;
    private String endTime;
    private Period period;
    private Integer total;
    private Integer remain;
    private Integer used;
    private SlotAvailStatus status;
}
