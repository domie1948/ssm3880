package com.controller;


import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.StringUtil;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;

import com.entity.GoodsInEntity;

import com.service.GoodsInService;
import com.entity.view.GoodsInView;
import com.service.GongyingshangService;
import com.entity.GongyingshangEntity;
import com.service.GoodsService;
import com.entity.GoodsEntity;

import com.utils.PageUtils;
import com.utils.R;

/**
 * 进货
 * 后端接口
 * @author
 * @email
 * @date 2021-04-29
*/
@RestController
@Controller
@RequestMapping("/goodsIn")
public class GoodsInController {
    private static final Logger logger = LoggerFactory.getLogger(GoodsInController.class);

    @Autowired
    private GoodsInService goodsInService;


    @Autowired
    private TokenService tokenService;
    @Autowired
    private DictionaryService dictionaryService;



    //级联表service
    @Autowired
    private GongyingshangService gongyingshangService;
    @Autowired
    private GoodsService goodsService;


    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
     
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(StringUtil.isNotEmpty(role) && "用户".equals(role)){
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        }
        params.put("orderBy","id");
        PageUtils page = goodsInService.queryPage(params);

        //字典表数据转换
        List<GoodsInView> list =(List<GoodsInView>)page.getList();
        for(GoodsInView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        GoodsInEntity goodsIn = goodsInService.selectById(id);
        if(goodsIn !=null){
            //entity转view
            GoodsInView view = new GoodsInView();
            BeanUtils.copyProperties( goodsIn , view );//把实体数据重构到view中

            //级联表
            GongyingshangEntity gongyingshang = gongyingshangService.selectById(goodsIn.getGongyingshangId());
            if(gongyingshang != null){
                BeanUtils.copyProperties( gongyingshang , view ,new String[]{ "id", "createDate"});//把级联的数据添加到view中,并排除id和创建时间字段
                view.setGongyingshangId(gongyingshang.getId());
            }
            //级联表
            GoodsEntity goods = goodsService.selectById(goodsIn.getGoodsId());
            if(goods != null){
                BeanUtils.copyProperties( goods , view ,new String[]{ "id", "createDate"});//把级联的数据添加到view中,并排除id和创建时间字段
                view.setGoodsId(goods.getId());
            }
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody GoodsInEntity goodsIn, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,goodsIn:{}",this.getClass().getName(),goodsIn.toString());
        GoodsEntity goodsEntity = goodsService.selectById(goodsIn.getGoodsId());
        if(goodsEntity == null){
            return R.error(511,"查询不到商品");
        }
        goodsEntity.setGoodsNumber(goodsEntity.getGoodsNumber() + goodsIn.getGoodsInNumber());
        goodsService.updateById(goodsEntity);
        goodsIn.setInsertTime(new Date());
        goodsIn.setCreateTime(new Date());
        goodsInService.insert(goodsIn);
        return R.ok();
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody GoodsInEntity goodsIn, HttpServletRequest request){
        logger.debug("update方法:,,Controller:{},,goodsIn:{}",this.getClass().getName(),goodsIn.toString());
        GoodsInEntity oldGoodsInEntity = goodsInService.selectById(goodsIn.getId()); // 查出原先的订单
        GoodsEntity oldGoodsEntity = goodsService.selectById(oldGoodsInEntity.getGoodsId());
        if(oldGoodsEntity == null){
            return R.error(511,"查询不到原先商品");
        }
        oldGoodsEntity.setGoodsNumber( oldGoodsEntity.getGoodsNumber() - oldGoodsInEntity.getGoodsInNumber());

        GoodsEntity goodsEntity = goodsService.selectById(goodsIn.getGoodsId());
        goodsEntity.setGoodsNumber(goodsEntity.getGoodsNumber() + goodsIn.getGoodsInNumber());

        goodsService.updateById(oldGoodsEntity);
        goodsService.updateById(goodsEntity);
        goodsInService.updateById(goodsIn);//根据id更新
        return R.ok();
    }



    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        goodsInService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }






}

