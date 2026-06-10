package com.ecommerceserver.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectResult;
import com.ecommerceserver.config.OSSConfig;
import com.ecommerceserver.service.OSSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class OSSServiceImpl implements OSSService {
    
    private final OSSConfig ossConfig;
    
    @Override
    public String uploadFile(MultipartFile file, String objectName) {
        OSS ossClient = createOSSClient();
        try {
            String bucketName = ossConfig.getBucketName();
            
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(getContentType(objectName));
            
            InputStream inputStream = file.getInputStream();
            PutObjectResult putObjectResult = ossClient.putObject(bucketName, objectName, inputStream, metadata);

            String url = getFileUrl(objectName);
            log.info("文件上传成功：{} -> {}", file.getOriginalFilename(), url);
            
            return url;
        } catch (Exception e) {
            log.error("文件上传失败：{}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败：" + e.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
    
    @Override
    public String uploadLocalFile(File localFile, String objectName) {
        OSS ossClient = createOSSClient();
        try {
            String bucketName = ossConfig.getBucketName();
            
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(getContentType(objectName));
            
            ossClient.putObject(bucketName, objectName, localFile, metadata);
            
            String url = getFileUrl(objectName);
            log.info("本地文件上传成功：{} -> {}", localFile.getName(), url);
            
            return url;
        } catch (Exception e) {
            log.error("本地文件上传失败：{}", e.getMessage(), e);
            throw new RuntimeException("本地文件上传失败：" + e.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
    
    @Override
    public boolean deleteFile(String objectName) {
        OSS ossClient = createOSSClient();
        try {
            String bucketName = ossConfig.getBucketName();
            ossClient.deleteObject(bucketName, objectName);
            log.info("文件删除成功：{}", objectName);
            return true;
        } catch (Exception e) {
            log.error("文件删除失败：{}", e.getMessage(), e);
            return false;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
    
    @Override
    public String getFileUrl(String objectName) {
        if (ossConfig.getCustomDomain() != null && !ossConfig.getCustomDomain().isEmpty()) {
            return "https://" + ossConfig.getCustomDomain() + "/" + objectName;
        }
        
        // 从endpoint中提取域名部分
        // endpoint格式：https://oss-cn-hangzhou.aliyuncs.com
        // 需要提取：oss-cn-hangzhou.aliyuncs.com
        String endpoint = ossConfig.getEndpoint();
        String domain = endpoint;
        
        // 移除协议前缀（http:// 或 https://）
        if (domain.startsWith("http://")) {
            domain = domain.substring(7);
        } else if (domain.startsWith("https://")) {
            domain = domain.substring(8);
        }
        
        // 生成OSS虚拟主机风格URL
        // 格式：https://{bucket}.{endpoint}/{objectName}
        return "https://" + ossConfig.getBucketName() + "." + domain + "/" + objectName;
    }
    
    private OSS createOSSClient() {
        return new OSSClientBuilder()
                .build(ossConfig.getEndpoint(), 
                       ossConfig.getAccessKeyId(), 
                       ossConfig.getAccessKeySecret());
    }
    
    private String getContentType(String objectName) {
        String extension = "";
        if (objectName.contains(".")) {
            extension = objectName.substring(objectName.lastIndexOf(".") + 1).toLowerCase();
        }
        
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default:
                return "application/octet-stream";
        }
    }
}