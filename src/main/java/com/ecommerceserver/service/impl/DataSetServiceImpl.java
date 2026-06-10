package com.ecommerceserver.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerceserver.Enum.DataSetEnum;
import com.ecommerceserver.Enum.UserEnum;
import com.ecommerceserver.constants.DatasetConstant;
import com.ecommerceserver.context.LoginContext;
import com.ecommerceserver.exception.GlobalException;
import com.ecommerceserver.mapper.DataSetMapper;
import com.ecommerceserver.mapper.DatasetFilesMapper;
import com.ecommerceserver.model.dto.DataSetDTO;
import com.ecommerceserver.model.entity.DataSet;
import com.ecommerceserver.model.entity.DatasetFiles;
import com.ecommerceserver.result.Result;
import com.ecommerceserver.service.DataSetService;
import com.ecommerceserver.service.VectorKnowledgeService;
import com.ecommerceserver.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class DataSetServiceImpl extends ServiceImpl<DataSetMapper, DataSet> implements DataSetService {

    @Autowired
    private DatasetFilesMapper datasetFilesMapper;

    @Autowired
    private VectorKnowledgeService vectorKnowledgeService;

    @Value("${tech-pilot-server.file.pathPrefix:/usr/local/project/smart-campus-service-assistant/data}")
    private String filePathPrefix;

    @Override
    public Boolean createDataSet(DataSetDTO dataSetDTO) {
        Long userId = LoginContext.getUserId();

        if (userId == null) {
            throw new GlobalException(Result.error(DatasetConstant.USER_ID_NOT_NULL));
        }
        if (StringUtils.isEmpty(dataSetDTO.getName())) {
            throw new GlobalException(Result.error(DatasetConstant.DATASET_NAME_EMPTY));
        }
        if (getDatasetByName(dataSetDTO.getName()) != null) {
            throw new GlobalException(Result.error(DatasetConstant.DATASET_ALREADY_EXIST));
        }

        DataSet entity = BeanUtil.copyProperties(dataSetDTO, DataSet.class);
        entity.setCreatedAt(new Date());
        entity.setUpdatedAt(new Date());
        entity.setDisabled(DataSetEnum.ON.getCode());
        entity.setUserId(userId);
        entity.setAppCount(0);
        entity.setDocCount(0);

        return this.save(entity);
    }

    private DataSet getDatasetByName(String name) {
        return this.getOne(new LambdaQueryWrapper<DataSet>()
                .eq(DataSet::getName, name));
    }

    @Override
    public boolean saveEntity(DataSet dataSet) {
        try {
            if (dataSet == null) {
                log.error("数据集实体不能为空");
                return false;
            }

            if (dataSet.getCreatedAt() == null) {
                dataSet.setCreatedAt(new Date());
            }
            if (dataSet.getUpdatedAt() == null) {
                dataSet.setUpdatedAt(new Date());
            }
            if (dataSet.getDisabled() == null) {
                dataSet.setDisabled(DataSetEnum.ON.getCode());
            }
            if (dataSet.getAppCount() == null) {
                dataSet.setAppCount(0);
            }
            if (dataSet.getDocCount() == null) {
                dataSet.setDocCount(0);
            }

            boolean result = this.save(dataSet);
            if (result) {
                log.info("数据集保存成功: {}, ID: {}", dataSet.getName(), dataSet.getId());
            } else {
                log.error("数据集保存失败: {}", dataSet.getName());
            }
            return result;
        } catch (Exception e) {
            log.error("保存数据集实体时发生异常", e);
            return false;
        }
    }

    @Override
    public boolean uploadFile(Long datasetId, MultipartFile file) {
        Long userId = LoginContext.getUserId();
        if (userId == null) {
            throw new GlobalException(Result.error(DatasetConstant.USER_ID_NOT_NULL));
        }

        if (file.isEmpty()) {
            throw new GlobalException(Result.error(DatasetConstant.FILE_NOT_NULL));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new GlobalException(Result.error(DatasetConstant.FILE_NAME_NOT_NULL));
        }

        String fileSuffix = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!fileSuffix.matches("pdf|docx?|xlsx?|pptx?|txt|md|html?|json")) {
            throw new GlobalException(Result.error(DatasetConstant.FILE_TYPE_NOT_SUPPORT));
        }
        
        try {
            DataSet dataSet = this.getById(datasetId);
            if (dataSet == null) {
                throw new GlobalException(Result.error(DatasetConstant.DATASET_NOT_EXIST));
            }
            
            String uploadPath = filePathPrefix + "/dataset/" + datasetId + "/";
            
            String savedFileName = handleFileNameConflict(uploadPath, originalFilename);
            
            boolean uploadSuccess = FileUtil.saveFile(file, uploadPath, savedFileName);

            if (!uploadSuccess) {
                log.error("文件上传失败");
                return false;
            }

            DatasetFiles datasetFile = DatasetFiles.builder()
                    .name(originalFilename)
                    .filePath(uploadPath + savedFileName)
                    .fileType(fileSuffix)
                    .fileSize(file.getSize())
                    .datasetId(datasetId)
                    .userId(userId)
                    .disabled(DataSetEnum.ON.getCode())
                    .createdAt(new Date())
                    .hitCount(0)
                    .build();

            int insert = datasetFilesMapper.insert(datasetFile);

            org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(uploadPath + savedFileName);
            boolean vectorSaveSuccess = vectorKnowledgeService.save(datasetId, resource);

            if (!vectorSaveSuccess) {
                log.error("向量库保存失败");
                datasetFilesMapper.deleteById(insert);
                return false;
            }

            log.info("文件上传成功: {}", originalFilename);
            return true;

        } catch (Exception e) {
            log.error("文件上传异常", e);
            e.printStackTrace();
            return false;
        }
    }
    
    private String handleFileNameConflict(String uploadPath, String originalFilename) {
        File file = new File(uploadPath + originalFilename);
        if (!file.exists()) {
            return originalFilename;
        }
        
        String baseName = originalFilename.substring(0, originalFilename.lastIndexOf("."));
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        
        int counter = 1;
        String newFileName;
        do {
            newFileName = baseName + "_" + counter + extension;
            file = new File(uploadPath + newFileName);
            counter++;
        } while (file.exists());
        
        log.info("文件名冲突，已重命名为: {}", newFileName);
        return newFileName;
    }

    @Override
    public List<DataSet> getUserDataSets(Long userId) {
        LambdaQueryWrapper<DataSet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataSet::getUserId, userId);
        wrapper.orderByDesc(DataSet::getCreatedAt);
        return this.list(wrapper);
    }

    @Override
    public boolean deleteDataSet(Long datasetId) {
        Long userId = LoginContext.getUserId();
        LoginContext.LoginUserDTO loginUser = LoginContext.getUser();
        if (userId == null) {
            throw new GlobalException(Result.error(DatasetConstant.USER_ID_NOT_NULL));
        }

        DataSet dataSet = this.getById(datasetId);
        if (dataSet == null) {
            throw new GlobalException(Result.error(DatasetConstant.DATASET_NOT_EXIST));
        }

        if (!dataSet.getUserId().equals(userId) && !loginUser.getUserType().equals(UserEnum.ADMIN.getCode())) {
            throw new GlobalException(Result.error(DatasetConstant.NO_PERMISSION));
        }

        boolean result = vectorKnowledgeService.deleteByDatasetId(datasetId);
        if (!result) {
            throw new GlobalException(Result.error(DatasetConstant.DELETE_VECTOR_KNOWLEDGE_FAILED));
        }

        LambdaQueryWrapper<DatasetFiles> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DatasetFiles::getDatasetId, datasetId);
        List<DatasetFiles> files = datasetFilesMapper.selectList(wrapper);

        for (DatasetFiles file : files) {
            File oldFile = new File(file.getFilePath());
            if (oldFile.exists()) {
                boolean deleted = oldFile.delete();
                if (!deleted) {
                    log.warn("删除物理文件失败: {}", file.getFilePath());
                }
            }
        }

        LambdaQueryWrapper<DatasetFiles> fileWrapper = new LambdaQueryWrapper<>();
        fileWrapper.eq(DatasetFiles::getDatasetId, datasetId);
        datasetFilesMapper.delete(fileWrapper);

        return this.removeById(datasetId);
    }

    @Override
    public boolean toggleDataSet(Long datasetId, Integer disabled) {
        Long userId = LoginContext.getUserId();
        if (userId == null) {
            throw new GlobalException(Result.error(DatasetConstant.USER_ID_NOT_NULL));
        }

        DataSet dataSet = this.getById(datasetId);
        if (dataSet == null) {
            throw new GlobalException(Result.error(DatasetConstant.DATASET_NOT_EXIST));
        }

        if (!dataSet.getUserId().equals(userId)) {
            throw new GlobalException(Result.error(DatasetConstant.NO_PERMISSION));
        }

        Integer oldStatus = dataSet.getDisabled();
        
        if (disabled.equals(DataSetEnum.OFF.getCode())) {
            boolean vectorDeleteSuccess = vectorKnowledgeService.deleteByDatasetId(datasetId);
            if (!vectorDeleteSuccess) {
                log.warn("删除数据集向量库失败: {}", datasetId);
                throw new GlobalException(Result.error(DatasetConstant.DELETE_VECTOR_KNOWLEDGE_FAILED));
            } else {
                log.info("数据集 {} 已禁用，相关向量数据已从向量库中删除", datasetId);
            }
        } else if (disabled.equals(DataSetEnum.ON.getCode())) {
            if (oldStatus != null && oldStatus.equals(DataSetEnum.OFF.getCode())) {
                log.info("数据集 {} 从禁用状态恢复，开始重建向量数据", datasetId);
                
                boolean rebuildSuccess = vectorKnowledgeService.rebuildByDatasetId(datasetId);
                if (!rebuildSuccess) {
                    log.error("数据集 {} 向量数据重建失败", datasetId);
                    throw new GlobalException(Result.error(DatasetConstant.REBUILD_VECTOR_KNOWLEDGE_FAILED));
                } else {
                    log.info("数据集 {} 向量数据重建成功", datasetId);
                }
            }
        }
        
        dataSet.setDisabled(disabled);
        dataSet.setDisabledAt(new Date());
        return this.updateById(dataSet);
    }
}