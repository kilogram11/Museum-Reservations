package com.museum.ai;

import com.museum.ai.dto.*;
import com.museum.ai.support.AiChatBlockCollector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("AiChatBlockCollector - deterministic blocks")
class AiChatBlockCollectorTest {

    @AfterEach
    void tearDown() {
        AiChatBlockCollector.clear();
    }

    @Test
    @DisplayName("queryTimes / listRecords / submit / cancel 生成结构化块")
    void recordsExpectedBlocks() {
        AiChatBlockCollector.begin();
        AiChatBlockCollector.recordQueryTimes(new QueryTimesData(
                "2026-08-19",
                List.of(new TimeSlot("tm1", "13:00", "14:00", Period.AFTERNOON, 50, 12, 38, SlotAvailStatus.AVAILABLE))
        ));
        AiChatBlockCollector.recordListRecords(new ListRecordsData(
                List.of(new BookingRecord("join_1", "2026-08-19", "tm1", "13:00", "14:00",
                        Period.AFTERNOON, BookingStatus.BOOKED, CheckinStatusAi.UNCHECKED,
                        "张三", "3101********1234", "博物馆", "南京东路 1 号"))
        ));
        AiChatBlockCollector.recordSubmitBooking(
                new SubmitBookingData(true, "tm1", 1, List.of()),
                "预约成功");
        AiChatBlockCollector.recordCancelBooking("join_1", "取消成功");

        List<ChatBlock> blocks = AiChatBlockCollector.snapshot();

        assertEquals(4, blocks.size());
        assertEquals("time_slots", blocks.get(0).getType());
        assertEquals("booking_records", blocks.get(1).getType());
        assertEquals("tips", blocks.get(2).getType());
        assertEquals("tips", blocks.get(3).getType());
    }
}
