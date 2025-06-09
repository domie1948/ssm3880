package com.service;

import com.baomidou.mybatisplus.service.IService;
import com.utils.PageUtils;
import com.entity.GoodsInEntity;
import java.util.Map;

/**
 * 进货 服务类
 * @author 
 * @since 2021-04-29
 */
public interface GoodsInService extends IService<GoodsInEntity> {

    /**
    * @param params 查询参数
    * @return 带分页的查询出来的数据
    */
     PageUtils queryPage(Map<String, Object> params);
}