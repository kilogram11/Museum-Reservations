package com.museum.common.utils;

import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;

import java.awt.Color;

/**
 * 二维码生成工具
 */
public class QRCodeUtil {

    /**
     * 生成 Base64 格式的二维码
     *
     * @param content 内容
     * @param width   宽度
     * @param height  高度
     * @return base64 image
     */
    public static String generateBase64(String content, int width, int height) {
        QrConfig config = new QrConfig(width, height);
        config.setBackColor(Color.WHITE);
        config.setForeColor(Color.BLACK);
        return QrCodeUtil.generateAsBase64(content, config, "png");
    }
}
