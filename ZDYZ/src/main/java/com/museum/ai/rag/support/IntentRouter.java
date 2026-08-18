package com.museum.ai.rag.support;

import com.museum.ai.rag.model.ChatIntent;
import org.springframework.stereotype.Component;

/**
 * 关键词意图分流：BOOKING / RULES / MIXED。
 */
@Component
public class IntentRouter {

    private static final String[] RULES_KEYWORDS = {
            "须知", "背包", "禁带", "开馆", "闭馆", "公告", "带水", "安检", "拍照",
            "迟到", "存包", "闪光灯", "自拍杆", "充电宝", "携带", "行李",
            "开放时间", "停止入馆", "文明参观", "预约规则", "禁限带", "馆规", "通知",
            "本馆", "规定", "谢绝", "允许带", "能不能带", "能否带", "禁限"
    };

    private static final String[] BOOKING_KEYWORDS = {
            "有票", "余票", "下单", "帮我订", "提交预约", "取消预约", "取消刚才",
            "我的预约", "预约记录", "timeMark", "identityId", "订一张", "帮我预约",
            "还有票吗", "能订吗"
    };

    public ChatIntent route(String message) {
        String text = message == null ? "" : message;
        boolean rules = containsAny(text, RULES_KEYWORDS);
        boolean booking = containsAny(text, BOOKING_KEYWORDS);
        // 「预约规则」已在 RULES；纯「预约」且无馆规词时倾向 BOOKING
        if (!booking && text.contains("预约") && !rules) {
            booking = true;
        }
        if (rules && booking) {
            return ChatIntent.MIXED;
        }
        if (rules) {
            return ChatIntent.RULES;
        }
        return ChatIntent.BOOKING;
    }

    private static boolean containsAny(String text, String[] keys) {
        for (String k : keys) {
            if (text.contains(k)) {
                return true;
            }
        }
        return false;
    }
}
