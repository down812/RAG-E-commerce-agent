package com.ecommerceserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerceserver.model.entity.DataSet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DataSetMapper extends BaseMapper<DataSet> {

    @Update("UPDATE dataset SET doc_count = doc_count + #{count} WHERE id = #{id}")
    int incrementDocCount(@Param("id") Long id, @Param("count") int count);

    @Update("UPDATE dataset SET doc_count = GREATEST(doc_count - #{count}, 0) WHERE id = #{id}")
    int decrementDocCount(@Param("id") Long id, @Param("count") int count);

    @Update("UPDATE dataset SET app_count = app_count + 1 WHERE id = #{id}")
    int incrementAppCount(@Param("id") Long id);
}