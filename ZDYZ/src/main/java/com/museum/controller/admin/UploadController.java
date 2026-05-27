package com.museum.controller.admin;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import com.museum.common.result.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传控制器
 */
@RestController
@RequestMapping("/admin/upload")
public class UploadController {

    /**
     * 图片上传
     */
    @PostMapping("/image")
    public Result uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        // 获取原文件名后缀
        String originalFilename = file.getOriginalFilename();
        String suffix = FileUtil.extName(originalFilename);

        // 生成新文件名
        String fileName = UUID.fastUUID().toString() + "." + suffix;

        // 获取项目根目录下的 files 目录
        String projectPath = System.getProperty("user.dir");
        String fileDir = projectPath + File.separator + "files";

        File dir = new File(fileDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 保存文件
        File dest = new File(fileDir + File.separator + fileName);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件保存失败");
        }

        // 返回可访问的 URL (需要在 WebMvcConfig 中配置映射)
        String url = "/files/" + fileName;

        Map<String, String> map = new HashMap<>();
        map.put("url", url);
        return Result.success("上传成功", map);
    }
}
