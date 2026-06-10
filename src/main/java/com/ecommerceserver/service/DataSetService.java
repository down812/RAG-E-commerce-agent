package com.ecommerceserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerceserver.model.dto.DataSetDTO;
import com.ecommerceserver.model.entity.DataSet;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DataSetService extends IService<DataSet> {

    /**
     * 创建数据集
     * @param dataSetDTO 数据集信息
     * @return 创建的数据集
     */
    Boolean createDataSet(DataSetDTO dataSetDTO);

    /**
     * 保存数据集实体（用于批量导入等场景）
     * @param dataSet 数据集实体
     * @return 保存结果
     */
    boolean saveEntity(DataSet dataSet);

    /**
     * 上传文件到数据集
     * @param datasetId 数据集ID
     * @param file 文件
     * @return 上传结果
     */
    boolean uploadFile(Long datasetId, MultipartFile file);

    /**
     * 获取用户的数据集列表
     * @param userId 用户ID
     * @return 数据集列表
     */
    List<DataSet> getUserDataSets(Long userId);

    /**
     * 删除数据集
     * @param datasetId 数据集ID
     * @return 删除结果
     */
    boolean deleteDataSet(Long datasetId);

    /**
     * 启用/禁用数据集
     * @param datasetId 数据集ID
     * @param disabled 禁用状态
     * @return 操作结果
     */
    boolean toggleDataSet(Long datasetId, Integer disabled);
}