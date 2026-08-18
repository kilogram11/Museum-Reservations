package com.museum.ai.rag.support;

/**
 * 去掉 HTML 标签与多余空白，得到可切片纯文本。
 */
public final class HtmlTextCleaner {

    private HtmlTextCleaner() {
    }

    public static String clean(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String noTags = raw.replaceAll("(?is)<[^>]+>", " ");
        String decoded = noTags
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"");
        return decoded.replaceAll("\\s+", " ").trim();
    }
}
