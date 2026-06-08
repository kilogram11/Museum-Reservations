package com.museum.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.common.exception.BusinessException;
import com.museum.entity.Identity;
import com.museum.entity.Join;
import com.museum.entity.Time;
import com.museum.service.JoinService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class JoinServiceImplIntegrationTest extends AdminBackendIntegrationTestBase {

    @Autowired
    private JoinService joinService;

    @Test
    void checkin_withSuccessfulBooking_shouldMarkCheckedIn() {
        insertIdentity(TEST_IDENTITY_ID + "_checkin", "测试核销用户");
        insertTime(TEST_TIME_MARK + "_checkin");
        insertJoin(TEST_JOIN_ID + "_checkin", TEST_IDENTITY_ID + "_checkin", TEST_TIME_MARK + "_checkin", 1, 0);

        joinService.checkin(TEST_JOIN_ID + "_checkin");

        Join updated = selectJoin(TEST_JOIN_ID + "_checkin");
        assertEquals(1, updated.getJoinIsCheckin());
        assertNotNull(updated.getJoinEditTime());
    }

    @Test
    void checkin_withAlreadyCheckedBooking_shouldThrowBusinessException() {
        insertIdentity(TEST_IDENTITY_ID + "_repeat", "测试重复核销用户");
        insertTime(TEST_TIME_MARK + "_repeat");
        insertJoin(TEST_JOIN_ID + "_repeat", TEST_IDENTITY_ID + "_repeat", TEST_TIME_MARK + "_repeat", 1, 1);

        assertThrows(BusinessException.class, () -> joinService.checkin(TEST_JOIN_ID + "_repeat"));
    }

    @Test
    void checkin_withMissingBooking_shouldThrowBusinessException() {
        assertThrows(BusinessException.class, () -> joinService.checkin(TEST_JOIN_ID + "_missing"));
    }

    @Test
    void adminList_withKeyword_shouldReturnMatchingBooking() {
        insertIdentity(TEST_IDENTITY_ID + "_list", "测试预约查询");
        insertTime(TEST_TIME_MARK + "_list");
        insertJoin(TEST_JOIN_ID + "_list", TEST_IDENTITY_ID + "_list", TEST_TIME_MARK + "_list", 1, 0);

        Page<Join> page = joinService.adminList("测试预约查询", 1, 10);

        assertTrue(page.getTotal() >= 1);
        assertTrue(page.getRecords().stream()
                .anyMatch(join -> (TEST_JOIN_ID + "_list").equals(join.getJoinId())));
    }

    private void insertIdentity(String identityId, String name) {
        Identity identity = new Identity();
        identity.setId(identityId);
        identity.setIdentityId(identityId);
        identity.setUserId(TEST_USER_ID);
        identity.setIdentityName(name);
        identity.setIdentityCard("34010019990101" + Math.abs(identityId.hashCode() % 10_000));
        identity.setIdentityMobile("1380000" + String.format("%04d", Math.abs(identityId.hashCode() % 10_000)));
        identity.setIdentityObj("{}");
        identity.setIdentityStatus(1);
        identity.setUserBanNum(0);
        identity.setPid("1");
        identityMapper.insert(identity);
    }

    private void insertTime(String timeMark) {
        Time time = new Time();
        time.setId(timeMark);
        time.setTimeId(timeMark);
        time.setTimeMark(timeMark);
        time.setTimeStart("09:00");
        time.setTimeEnd("10:00");
        time.setLimitCnt(10);
        time.setSuccCnt(0);
        time.setStatus(1);
        time.setIsLimit(1);
        time.setAddTime(System.currentTimeMillis());
        time.setEditTime(System.currentTimeMillis());
        time.setPid("1");
        timeMapper.insert(time);
    }

    private void insertJoin(String joinId, String identityId, String timeMark, Integer joinStatus,
                            Integer checkinStatus) {
        Join join = new Join();
        join.setId(joinId);
        join.setJoinId(joinId);
        join.setIdentityId(identityId);
        join.setUserId(TEST_USER_ID);
        join.setJoinMeetDay("2026-07-01");
        join.setTimeMark(timeMark);
        join.setJoinStartTime(System.currentTimeMillis() + 86_400_000L);
        join.setJoinCompleteEndTime("2026-07-01 10:00:00");
        join.setJoinStatus(joinStatus);
        join.setJoinForms("[]");
        join.setJoinIsCheckin(checkinStatus);
        join.setJoinAddTime(System.currentTimeMillis());
        join.setJoinEditTime(System.currentTimeMillis());
        join.setPid("1");
        joinMapper.insert(join);
    }

    private Join selectJoin(String joinId) {
        return joinMapper.selectOne(new QueryWrapper<Join>().eq("JOIN_ID", joinId));
    }
}
