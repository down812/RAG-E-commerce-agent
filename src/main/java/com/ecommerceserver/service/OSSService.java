package com.ecommerceserver.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.File;

public interface OSSService {
    
    /**
     * 上传文件到OSS
     * @param file 文件
     * @param objectName OSS对象名（路径）
     * @return OSS访问URL
     */
    String uploadFile(MultipartFile file, String objectName);
    
    /**
     * 上传本地文件到OSS
     * @param localFile 本地文件
     * @param objectName OSS对象名
     * @return OSS访问URL
     */
    String uploadLocalFile(File localFile, String objectName);
    
    /**
     * 删除OSS文件
     * @param objectName OSS对象名
     * @return 是否删除成功
     */
    boolean deleteFile(String objectName);
    
    /**
     * 获取文件访问URL
     * @param objectName OSS对象名
     * @return 访问URL
     */
    String getFileUrl(String objectName);
}