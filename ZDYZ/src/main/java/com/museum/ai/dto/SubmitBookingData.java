package com.museum.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitBookingData {
    private boolean booked;
    private String timeMark;
    private int visitorCount;
    /** 当前业务不返回预约号；预留扩展 */
    private List<String> joinIds;
}
