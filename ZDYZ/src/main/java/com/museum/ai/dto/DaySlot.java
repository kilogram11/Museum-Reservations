package com.museum.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DaySlot {
    private String day;
    private String week;
    private DayOpenStatus status;
}
