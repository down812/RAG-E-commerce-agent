package com.ecommerceserver.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author dawn
 * @date 2024-09-14 10:24
 * 文件工具
 */
@Slf4j
@Component
public class FileUtil {
    /**
     * 拼接目录
     * @param name 名称
     * @return 拼接后的目录路径
     */
    public static String pathSpliceBatch(String... name) {
        StringBuilder path = new StringBuilder();
        Arrays.stream(name).collect(Collectors.toList()).forEach(directory -> path.append(directory).append("/"));
        return path.toString();
    }

    /**
     * 文件上传
     *
     * @param multiFile      文件
     * @param uploadPath     要存储文件的路径
     * @param uploadFileName 要存储文件的文件名称
     * @return
     */
    public static boolean saveFile(MultipartFile multiFile, String uploadPath, String uploadFileName) {
        //构建文件对象
        File file = new File(uploadPath);
        //文件目录不存在则创建目录
        if (!file.exists()) {
            boolean mkdirs = file.mkdirs();
            if (!mkdirs) {
                log.error("创建文件夹异常");
                return false;
            }
        }
        // 构建完整的文件路径
        File serverFile = new File(file, uploadFileName);
        try(InputStream inputStream = multiFile.getInputStream();
            FileOutputStream outputStream = new FileOutputStream(uploadPath + uploadFileName)) {
//
            int copy = FileCopyUtils.copy(inputStream, outputStream);
            log.info("上传成功,文件大小：{}", copy);
            return true;
        } catch (IOException e) {
            log.error("文件上传异常", e);
            e.printStackTrace();
        }
        return false;
    }


    /**
     * 获取文件后缀
     * @param originalFilename
     * @return
     */
    public static String getFileSuffix(String originalFilename) {
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }
}
