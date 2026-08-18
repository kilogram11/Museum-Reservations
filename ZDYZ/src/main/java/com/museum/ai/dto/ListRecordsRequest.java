package com.museum.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListRecordsRequest {
    /** 可选，yyyy-MM-dd */
    private String day;
    /** 可选，BOOKED | CANCELLED */
    private BookingStatus status;
}
