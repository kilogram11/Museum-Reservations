package com.museum.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.common.dto.ActivityAddDTO;
import com.museum.common.dto.ActivityEditDTO;
import com.museum.common.exception.BusinessException;
import com.museum.entity.Activity;
import com.museum.service.ActivityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ActivityServiceImplIntegrationTest extends AdminBackendIntegrationTestBase {

    @Autowired
    private ActivityService activityService;

    @Test
    void addActivity_withValidData_shouldPersistActivity() {
        activityService.addActivity(validActivityDTO(TEST_PREFIX + "activity_add", 0));

        Activity saved = selectActivityByTitle(TEST_PREFIX + "activity_add");
        assertNotNull(saved);
        assertEquals(0, saved.getActivityStatus());
        assertEquals(TEST_ADMIN_ID, saved.getAdminId());
        assertTrue(saved.getActivityPic().contains("https://example.com/activity.png"));
    }

    @Test
    void addActivity_withBlankTitle_shouldThrowBusinessException() {
        ActivityAddDTO dto = validActivityDTO(" ", 0);

        assertThrows(BusinessException.class, () -> activityService.addActivity(dto));
    }

    @Test
    void editActivity_withExistingId_shouldUpdateActivity() {
        activityService.addActivity(validActivityDTO(TEST_PREFIX + "activity_edit_old", 0));
        Activity saved = selectActivityByTitle(TEST_PREFIX + "activity_edit_old");

        ActivityEditDTO editDTO = validActivityEditDTO(saved.getId(), TEST_PREFIX + "activity_edit_new", 1);
        activityService.editActivity(editDTO);

        Activity updated = activityMapper.selectById(saved.getId());
        assertEquals(TEST_PREFIX + "activity_edit_new", updated.getActivityTitle());
        assertEquals(1, updated.getActivityStatus());
    }

    @Test
    void editActivity_withMissingId_shouldThrowBusinessException() {
        ActivityEditDTO editDTO = validActivityEditDTO("not_exists_id", TEST_PREFIX + "activity_missing", 0);

        assertThrows(BusinessException.class, () -> activityService.editActivity(editDTO));
    }

    @Test
    void dataList_withKeyword_shouldReturnMatchingActivity() {
        activityService.addActivity(validActivityDTO(TEST_PREFIX + "activity_query", 0));

        Page<Activity> page = activityService.dataList(TEST_PREFIX + "activity_query", 1, 10);

        assertTrue(page.getTotal() >= 1);
        assertTrue(page.getRecords().stream()
                .anyMatch(activity -> (TEST_PREFIX + "activity_query").equals(activity.getActivityTitle())));
    }

    private Activity selectActivityByTitle(String title) {
        return activityMapper.selectOne(new QueryWrapper<Activity>().eq("ACTIVITY_TITLE", title));
    }

    private ActivityAddDTO validActivityDTO(String title, Integer status) {
        ActivityAddDTO dto = new ActivityAddDTO();
        dto.setActivityTitle(title);
        dto.setStartDate("2026-07-01");
        dto.setEndDate("2026-07-02");
        dto.setStatus(status);
        dto.setAdminId(TEST_ADMIN_ID);
        dto.setContent(List.of(
                new ActivityAddDTO.ContentItem("text", "测试活动内容"),
                new ActivityAddDTO.ContentItem("img", "https://example.com/activity.png")));
        return dto;
    }

    private ActivityEditDTO validActivityEditDTO(String id, String title, Integer status) {
        ActivityEditDTO dto = new ActivityEditDTO();
        dto.setId(id);
        dto.setActivityTitle(title);
        dto.setStartDate("2026-07-03");
        dto.setEndDate("2026-07-04");
        dto.setStatus(status);
        dto.setAdminId(TEST_ADMIN_ID);
        dto.setContent(List.of(new ActivityAddDTO.ContentItem("text", "编辑后的测试活动内容")));
        return dto;
    }
}
