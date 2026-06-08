package com.museum.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.common.dto.MuseumAddDTO;
import com.museum.common.dto.MuseumEditDTO;
import com.museum.common.exception.BusinessException;
import com.museum.entity.Museum;
import com.museum.service.MuseumService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MuseumServiceImplIntegrationTest extends AdminBackendIntegrationTestBase {

    @Autowired
    private MuseumService museumService;

    @Test
    void addMuseum_withValidData_shouldPersistMuseum() {
        MuseumAddDTO dto = validMuseumDTO(TEST_PREFIX + "museum_add", 0);

        museumService.addMuseum(dto);

        Museum saved = selectMuseumByTitle(TEST_PREFIX + "museum_add");
        assertNotNull(saved);
        assertEquals(0, saved.getMuseumStatus());
        assertEquals(TEST_ADMIN_ID, saved.getAdminId());
        assertEquals("测试地址", saved.getAddress());
    }

    @Test
    void addMuseum_withBlankTitle_shouldThrowBusinessException() {
        MuseumAddDTO dto = validMuseumDTO(" ", 0);

        assertThrows(BusinessException.class, () -> museumService.addMuseum(dto));
    }

    @Test
    void editMuseum_withExistingId_shouldUpdateMuseum() {
        museumService.addMuseum(validMuseumDTO(TEST_PREFIX + "museum_edit_old", 0));
        Museum saved = selectMuseumByTitle(TEST_PREFIX + "museum_edit_old");

        MuseumEditDTO editDTO = validMuseumEditDTO(saved.getId(), TEST_PREFIX + "museum_edit_new", 1);
        museumService.editMuseum(editDTO);

        Museum updated = museumMapper.selectById(saved.getId());
        assertEquals(TEST_PREFIX + "museum_edit_new", updated.getMuseumTitle());
        assertEquals(1, updated.getMuseumStatus());
        assertEquals(120, updated.getMuseumMaxJoinCnt());
    }

    @Test
    void editMuseum_withMissingId_shouldThrowBusinessException() {
        MuseumEditDTO editDTO = validMuseumEditDTO("not_exists_id", TEST_PREFIX + "museum_missing", 0);

        assertThrows(BusinessException.class, () -> museumService.editMuseum(editDTO));
    }

    @Test
    void dataList_withKeyword_shouldReturnMatchingMuseum() {
        museumService.addMuseum(validMuseumDTO(TEST_PREFIX + "museum_query", 0));

        Page<Museum> page = museumService.dataList(TEST_PREFIX + "museum_query", 1, 10);

        assertTrue(page.getTotal() >= 1);
        assertTrue(page.getRecords().stream()
                .anyMatch(museum -> (TEST_PREFIX + "museum_query").equals(museum.getMuseumTitle())));
    }

    private Museum selectMuseumByTitle(String title) {
        return museumMapper.selectOne(new QueryWrapper<Museum>().eq("MUSEUM_TITLE", title));
    }

    private MuseumAddDTO validMuseumDTO(String title, Integer status) {
        MuseumAddDTO dto = new MuseumAddDTO();
        dto.setMuseumTitle(title);
        dto.setMuseumDesc("测试场馆简介");
        dto.setMuseumCover("https://example.com/test.png");
        dto.setMuseumImgs(List.of("https://example.com/test.png"));
        dto.setMuseumContent("测试场馆详情");
        dto.setMuseumAddress("测试地址");
        dto.setMuseumPhone("0551-12345678");
        dto.setMuseumTraffic("地铁测试线");
        dto.setMuseumStatus(status);
        dto.setAdminId(TEST_ADMIN_ID);
        dto.setMuseumMaxJoinCnt(100);
        dto.setMuseumBookSet(7);
        dto.setStartDate("2026-07-01");
        dto.setEndDate("2026-07-01");
        dto.setTimes(List.of(new MuseumAddDTO.TimeTemplate("09:00", "10:00", 50)));
        dto.setLatitude(31.8206);
        dto.setLongitude(117.2272);
        dto.setAddress("结构化测试地址");
        return dto;
    }

    private MuseumEditDTO validMuseumEditDTO(String id, String title, Integer status) {
        MuseumEditDTO dto = new MuseumEditDTO();
        dto.setId(id);
        dto.setMuseumTitle(title);
        dto.setMuseumDesc("编辑后的测试场馆简介");
        dto.setMuseumCover("https://example.com/edit.png");
        dto.setMuseumImgs(List.of("https://example.com/edit.png"));
        dto.setMuseumContent("编辑后的测试场馆详情");
        dto.setMuseumAddress("编辑后的测试地址");
        dto.setMuseumPhone("0551-87654321");
        dto.setMuseumTraffic("公交测试线");
        dto.setMuseumStatus(status);
        dto.setAdminId(TEST_ADMIN_ID);
        dto.setMuseumMaxJoinCnt(120);
        dto.setMuseumBookSet(5);
        dto.setStartDate("2026-07-02");
        dto.setEndDate("2026-07-02");
        dto.setTimes(List.of(new MuseumAddDTO.TimeTemplate("10:00", "11:00", 60)));
        dto.setLatitude(31.8210);
        dto.setLongitude(117.2280);
        dto.setAddress("编辑后的结构化地址");
        return dto;
    }
}
