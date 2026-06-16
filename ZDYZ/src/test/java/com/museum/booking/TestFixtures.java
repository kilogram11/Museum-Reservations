package com.museum.booking;

import com.museum.common.enums.IdentityStatus;
import com.museum.entity.Day;
import com.museum.entity.Identity;
import com.museum.entity.Time;

/**
 * 测试夹具工具类 —— 提供构建模拟领域对象的工厂方法，
 * 供 JoinServiceImplTest 等测试类共用，避免重复代码。
 */
final class TestFixtures {

    private TestFixtures() {
    }

    static Time buildTime(String timeMark, String dayId, int limitCnt, int succCnt) {
        Time time = new Time();
        time.setTimeMark(timeMark);
        time.setDayId(dayId);
        time.setStatus(1);
        time.setLimitCnt(limitCnt);
        time.setSuccCnt(succCnt);
        time.setTimeStart("09:00");
        time.setTimeEnd("11:00");
        time.setMuseumId("MUSEUM_001");
        return time;
    }

    static Day buildDay(String dayId, String dayStr) {
        Day day = new Day();
        day.setDayId(dayId);
        day.setDay(dayStr);
        day.setStatus(1);
        day.setMuseumId("MUSEUM_001");
        return day;
    }

    static Identity buildIdentity(String identityId, String name, IdentityStatus status) {
        Identity identity = new Identity();
        identity.setId("1");
        identity.setIdentityId(identityId);
        identity.setIdentityName(name);
        identity.setIdentityCard("110101199001011234");
        identity.setIdentityMobile("13800138000");
        identity.setIdentityStatus(status.getCode());
        identity.setUserId("[\"user123\"]");
        return identity;
    }
}
