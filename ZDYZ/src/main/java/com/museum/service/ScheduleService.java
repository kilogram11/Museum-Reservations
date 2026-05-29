package com.museum.service;

import com.museum.common.dto.MuseumAddDTO;

import java.util.List;

public interface ScheduleService {
    void initMuseumSchedule(String museumId, String startDate, String endDate,
                            List<MuseumAddDTO.TimeTemplate> templates);

    void updateMuseumScheduleStatus(String museumId, Integer status);

    void initActivitySchedule(String activityId, String startDate, String endDate);

    void updateActivityScheduleStatus(String activityId, Integer status);
}
