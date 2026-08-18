package com.museum.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRecord {
    private String joinId;
    private String day;
    private String timeMark;
    private String startTime;
    private String endTime;
    private Period period;
    private BookingStatus status;
    private CheckinStatusAi checkin;
    private String visitorName;
    private String visitorCardMasked;
    private String museumTitle;
    private String museumAddress;
}
