package com.museum.ai.rag.loader;

import com.museum.ai.rag.model.RagDocument;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 加载 classpath:rag/visit-rules.md（小程序须知镜像）。
 */
@Component
public class StaticRulesLoader {

    public static final String SOURCE_TYPE = "static_rules";
    private static final String RESOURCE = "rag/visit-rules.md";

    public List<RagDocument> load() {
        List<RagDocument> docs = new ArrayList<>();
        try {
            ClassPathResource resource = new ClassPathResource(RESOURCE);
            if (!resource.exists()) {
                return docs;
            }
            String raw = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String withoutComment = raw.replaceFirst("(?s)<!--.*?-->", "").trim();
            String[] sections = withoutComment.split("(?m)^# ");
            int i = 0;
            for (String section : sections) {
                String s = section.trim();
                if (s.isEmpty()) {
                    continue;
                }
                int nl = s.indexOf('\n');
                String title;
                String body;
                if (nl < 0) {
                    title = s;
                    body = s;
                } else {
                    title = s.substring(0, nl).trim();
                    body = s.substring(nl + 1).trim();
                }
                if (body.isBlank()) {
                    body = title;
                }
                docs.add(new RagDocument(
                        "static_rules:" + i,
                        SOURCE_TYPE,
                        "visit-rules#" + i,
                        title.isEmpty() ? "参观须知" : title,
                        body,
                        null
                ));
                i++;
            }
        } catch (IOException e) {
            throw new IllegalStateException("无法加载 RAG 静态须知: " + RESOURCE, e);
        }
        return docs;
    }
}
