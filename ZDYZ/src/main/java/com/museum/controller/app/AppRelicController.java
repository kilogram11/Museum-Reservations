package com.museum.controller.app;

import com.museum.common.result.Result;
import com.museum.entity.Relic;
import com.museum.service.RelicService;
import com.museum.service.impl.RelicOnnxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 移动端文物识别控制器 (ONNX 版)
 */
@RestController
@RequestMapping("/app/relic")
public class AppRelicController {

    @Autowired
    private RelicService relicService;

    @Autowired
    private RelicOnnxService relicOnnxService;

    // 类别映射 (索引 -> 默认名称，以防数据库未命中)
    private static final String[] CLASS_NAMES = {
            "马踏飞燕", "四羊方尊", "长信宫灯", "后母戊鼎", "唐三彩"
    };

    @PostMapping("/identify")
    public Result identify(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(500, "请上传图片");
        }

        try {
            // 直接调用 Service 处理流，无需保存临时文件
            int classId = relicOnnxService.predict(file.getInputStream());

            if (classId != -1) {
                // 查询数据库
                String idStr = String.valueOf(classId);
                Relic relic = relicService.getById(idStr);

                // 构造返回数据
                Map<String, Object> finalResult = new HashMap<>();

                // 构造识别基础信息
                Map<String, Object> recognition = new HashMap<>();
                recognition.put("id", classId);
                String label = (classId < CLASS_NAMES.length) ? CLASS_NAMES[classId] : "Unknown";
                recognition.put("label", label);
                finalResult.put("recognition", recognition);

                // 构造详情信息
                finalResult.put("detail", relic);

                // 特殊处理：如果是“马踏飞燕”，返回3D模型路径
                if ("马踏飞燕".equals(label) || (relic != null && "马踏飞燕".equals(relic.getRelicName()))) {
                    // 请确保 src/main/resources/static/models/matafeiyan.glb 存在
                    // 或者您可以将文件上传到 OSS 并返回完整 URL
                    // 小程序端访问地址示例: http://localhost:8081/models/matafeiyan.glb
                    finalResult.put("modelUrl", "/models/matafeiyan.glb");
                }

                return Result.success("识别成功", finalResult);
            } else {
                return Result.error(500, "模型识别失败，请重试");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "系统异常: " + e.getMessage());
        }
    }
}
