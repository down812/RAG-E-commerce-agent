package com.ecommerceserver.service;

import java.io.File;

public interface ProductDataImportService {
    
    /**
     * 批量导入所有分类的商品数据
     * @param dataDirectory 数据目录路径（如：/data）
     * @return 导入结果统计
     */
    ImportResult importAllProducts(String dataDirectory);
    
    /**
     * 导入单个JSON文件
     * @param jsonFile JSON文件
     * @param category 分类名称
     * @return 是否导入成功
     */
    boolean importSingleProduct(File jsonFile, String category);
    
    /**
     * 迁移本地图片到OSS
     * @param imageDirectory 图片目录路径
     * @param dataDirectory 数据目录路径（用于获取分类信息）
     * @return 迁移结果统计
     */
    ImportResult migrateImagesToOSS(String imageDirectory, String dataDirectory);
    
    /**
     * 导入单个分类的商品
     * @param categoryName 分类名称
     * @param dataDirectory 基础数据目录
     * @return 导入结果统计
     */
    ImportResult importCategory(String categoryName, String dataDirectory);
    
    /**
     * 导入结果统计类
     */
    class ImportResult {
        private int successCount = 0;
        private int failCount = 0;
        private int totalCount = 0;
        private StringBuilder errors = new StringBuilder();
        
        public void addSuccess() {
            this.successCount++;
            this.totalCount++;
        }
        
        public void addFail(String error) {
            this.failCount++;
            this.totalCount++;
            this.errors.append(error).append("\n");
        }
        
        public void reset() {
            this.successCount = 0;
            this.failCount = 0;
            this.totalCount = 0;
            this.errors = new StringBuilder();
        }
        
        public int getSuccessCount() { return successCount; }
        public int getFailCount() { return failCount; }
        public int getTotalCount() { return totalCount; }
        public String getErrors() { return errors.toString(); }
        
        @Override
        public String toString() {
            return String.format("导入统计：成功=%d, 失败=%d, 总计=%d", 
                    successCount, failCount, totalCount);
        }
    }
}