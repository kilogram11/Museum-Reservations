package com.museum.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.museum.common.dto.NewsAddDTO;
import com.museum.common.dto.NewsEditDTO;
import com.museum.common.exception.BusinessException;
import com.museum.entity.News;
import com.museum.service.NewsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class NewsServiceImplIntegrationTest extends AdminBackendIntegrationTestBase {

    @Autowired
    private NewsService newsService;

    @Test
    void addNews_withValidData_shouldPersistNews() {
        newsService.addNews(validNewsDTO(TEST_PREFIX + "news_add", "测试公告内容", 1));

        News saved = selectNewsByTitle(TEST_PREFIX + "news_add");
        assertNotNull(saved);
        assertEquals(1, saved.getNewsStatus());
        assertEquals(0, saved.getNewsViewCnt());
    }

    @Test
    void addNews_withBlankTitle_shouldThrowBusinessException() {
        NewsAddDTO dto = validNewsDTO(" ", "测试公告内容", 1);

        assertThrows(BusinessException.class, () -> newsService.addNews(dto));
    }

    @Test
    void viewNews_withExistingId_shouldIncreaseViewCount() {
        newsService.addNews(validNewsDTO(TEST_PREFIX + "news_view", "测试公告内容", 1));
        News saved = selectNewsByTitle(TEST_PREFIX + "news_view");

        News viewed = newsService.viewNews(saved.getId());

        assertEquals(saved.getNewsViewCnt() + 1, viewed.getNewsViewCnt());
    }

    @Test
    void editNews_withExistingId_shouldUpdateNews() {
        newsService.addNews(validNewsDTO(TEST_PREFIX + "news_edit_old", "测试公告内容", 1));
        News saved = selectNewsByTitle(TEST_PREFIX + "news_edit_old");

        NewsEditDTO editDTO = new NewsEditDTO();
        editDTO.setId(saved.getId());
        editDTO.setNewsTitle(TEST_PREFIX + "news_edit_new");
        editDTO.setNewsDesc("编辑后的公告内容");
        editDTO.setNewsStatus(0);
        newsService.editNews(editDTO);

        News updated = newsMapper.selectById(saved.getId());
        assertEquals(TEST_PREFIX + "news_edit_new", updated.getNewsTitle());
        assertEquals("编辑后的公告内容", updated.getNewsDesc());
        assertEquals(0, updated.getNewsStatus());
    }

    @Test
    void dataList_withKeyword_shouldReturnMatchingNews() {
        newsService.addNews(validNewsDTO(TEST_PREFIX + "news_query", "测试公告内容", 1));

        Page<News> page = newsService.dataList(TEST_PREFIX + "news_query", 1, 10);

        assertTrue(page.getTotal() >= 1);
        assertTrue(page.getRecords().stream()
                .anyMatch(news -> (TEST_PREFIX + "news_query").equals(news.getNewsTitle())));
    }

    private News selectNewsByTitle(String title) {
        return newsMapper.selectOne(new QueryWrapper<News>().eq("NEWS_TITLE", title));
    }

    private NewsAddDTO validNewsDTO(String title, String desc, Integer status) {
        NewsAddDTO dto = new NewsAddDTO();
        dto.setNewsTitle(title);
        dto.setNewsDesc(desc);
        dto.setNewsStatus(status);
        return dto;
    }
}
