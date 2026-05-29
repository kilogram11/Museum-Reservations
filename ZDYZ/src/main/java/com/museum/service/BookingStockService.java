package com.museum.service;

import com.museum.entity.Time;

public interface BookingStockService {
    void checkSufficient(Time time, int need);

    void deduct(Time time, int need);

    void rollback(String timeMark);
}
