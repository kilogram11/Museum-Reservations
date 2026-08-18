package com.museum.ai.rag.loader;

import com.museum.ai.rag.model.RagDocument;
import com.museum.ai.rag.support.HtmlTextCleaner;
import com.museum.entity.News;
import com.museum.service.NoticeService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 从全量可见公告装库；只调用 {@link NoticeService#listVisibleForRag()}，禁止 appList。
 */
@Component
public class NewsRagLoader {

    public static final String SOURCE_TYPE = "news";

    private final NoticeService noticeService;

    public NewsRagLoader(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    public List<RagDocument> load() {
        List<News> rows = noticeService.listVisibleForRag();
        List<RagDocument> docs = new ArrayList<>();
        if (rows == null) {
            return docs;
        }
        for (News news : rows) {
            String title = news.getNewsTitle() == null ? "" : news.getNewsTitle().trim();
            String body = HtmlTextCleaner.clean(news.getNewsDesc());
            if (!StringUtils.hasText(title) || !StringUtils.hasText(body)) {
                continue;
            }
            String id = news.getNewsId() != null ? news.getNewsId() : news.getId();
            docs.add(new RagDocument(
                    "news:" + id,
                    SOURCE_TYPE,
                    id,
                    title,
                    body,
                    news.getNewsAddTime()
            ));
        }
        return docs;
    }
}
