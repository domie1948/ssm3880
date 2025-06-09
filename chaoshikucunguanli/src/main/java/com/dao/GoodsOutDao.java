package com.dao;

import com.entity.GoodsOutEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.plugins.pagination.Pagination;

import org.apache.ibatis.annotations.Param;
import com.entity.view.GoodsOutView;

/**
 * 销售 Dao 接口
 *
 * @author 
 * @since 2021-04-29
 */
public interface GoodsOutDao extends BaseMapper<GoodsOutEntity> {

   List<GoodsOutView> selectListView(Pagination page,@Param("params")Map<String,Object> params);

}
