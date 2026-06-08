package com.museum.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.common.exception.BusinessException;
import com.museum.entity.Identity;
import com.museum.service.BlacklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class BlacklistServiceImplIntegrationTest extends AdminBackendIntegrationTestBase {

    @Autowired
    private BlacklistService blacklistService;

    @Test
    void add_withExistingIdentity_shouldMoveIdentityToBlacklist() {
        insertNormalIdentity(TEST_IDENTITY_ID + "_add", "测试拉黑用户");
        long endTime = System.currentTimeMillis() + 86_400_000L;

        blacklistService.add(TEST_IDENTITY_ID + "_add", "测试拉黑原因", endTime);

        Identity updated = selectIdentity(TEST_IDENTITY_ID + "_add");
        assertEquals(0, updated.getIdentityStatus());
        assertNotNull(updated.getBlackStartTime());
        assertEquals(endTime, updated.getBlackEndTime());
        assertTrue(updated.getIdentityObj().contains("测试拉黑原因"));
    }

    @Test
    void add_withMissingIdentity_shouldThrowBusinessException() {
        assertThrows(BusinessException.class,
                () -> blacklistService.add(TEST_IDENTITY_ID + "_missing", "原因", System.currentTimeMillis()));
    }

    @Test
    void updateEndTime_withExistingIdentity_shouldChangeEndTime() {
        insertNormalIdentity(TEST_IDENTITY_ID + "_update_time", "测试更新时间用户");
        blacklistService.add(TEST_IDENTITY_ID + "_update_time", "测试拉黑原因", System.currentTimeMillis() + 1000L);

        long newEndTime = System.currentTimeMillis() + 172_800_000L;
        blacklistService.updateEndTime(TEST_IDENTITY_ID + "_update_time", newEndTime);

        assertEquals(newEndTime, selectIdentity(TEST_IDENTITY_ID + "_update_time").getBlackEndTime());
    }

    @Test
    void remove_withBlacklistedIdentity_shouldRestoreNormalStatus() {
        insertNormalIdentity(TEST_IDENTITY_ID + "_remove", "测试取消拉黑用户");
        blacklistService.add(TEST_IDENTITY_ID + "_remove", "测试拉黑原因", System.currentTimeMillis() + 1000L);

        blacklistService.remove(TEST_IDENTITY_ID + "_remove");

        Identity restored = selectIdentity(TEST_IDENTITY_ID + "_remove");
        assertEquals(1, restored.getIdentityStatus());
        assertNull(restored.getBlackStartTime());
        assertNull(restored.getBlackEndTime());
    }

    @Test
    void list_withKeyword_shouldReturnBlacklistedIdentity() {
        insertNormalIdentity(TEST_IDENTITY_ID + "_list", "测试黑名单查询");
        blacklistService.add(TEST_IDENTITY_ID + "_list", "测试拉黑原因", System.currentTimeMillis() + 1000L);

        Page<Identity> page = blacklistService.list("测试黑名单查询", 1, 10, 0);

        assertTrue(page.getTotal() >= 1);
        assertTrue(page.getRecords().stream()
                .anyMatch(identity -> (TEST_IDENTITY_ID + "_list").equals(identity.getIdentityId())));
    }

    private void insertNormalIdentity(String identityId, String name) {
        Identity identity = new Identity();
        identity.setId(identityId);
        identity.setIdentityId(identityId);
        identity.setUserId(TEST_USER_ID);
        identity.setIdentityName(name);
        identity.setIdentityCard("34010020000101" + Math.abs(identityId.hashCode() % 10_000));
        identity.setIdentityMobile("1390000" + String.format("%04d", Math.abs(identityId.hashCode() % 10_000)));
        identity.setIdentityObj("{}");
        identity.setIdentityStatus(1);
        identity.setUserBanNum(0);
        identity.setPid("1");
        identityMapper.insert(identity);
    }

    private Identity selectIdentity(String identityId) {
        return identityMapper.selectOne(new QueryWrapper<Identity>().eq("IDENTITY_ID", identityId));
    }
}
