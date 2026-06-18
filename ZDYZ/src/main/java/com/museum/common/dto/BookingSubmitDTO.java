package com.museum.common.dto;

import lombok.Data;

import java.util.List;

@Data
public class BookingSubmitDTO {
    /**
     * 时段标识 (通常包含 日期、时段ID)
     */
    private String timeMark;

    /**
     * 选中的游客ID列表
     */
    private List<String> identityIds;
}
