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

import com.entity.GoodsOutEntity;

import com.service.GoodsOutService;
import com.entity.view.GoodsOutView;
import com.service.GoodsService;
import com.entity.GoodsEntity;
import com.service.HuiyuanService;
import com.entity.HuiyuanEntity;

import com.utils.PageUtils;
import com.utils.R;

/**
 * 销售
 * 后端接口
 * @author
 * @email
 * @date 2021-04-29
*/
@RestController
@Controller
@RequestMapping("/goodsOut")
public class GoodsOutController {
    private static final Logger logger = LoggerFactory.getLogger(GoodsOutController.class);

    @Autowired
    private GoodsOutService goodsOutService;


    @Autowired
    private TokenService tokenService;
    @Autowired
    private DictionaryService dictionaryService;



    //级联表service
    @Autowired
    private GoodsService goodsService;
    @Autowired
    private HuiyuanService huiyuanService;


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
        PageUtils page = goodsOutService.queryPage(params);

        //字典表数据转换
        List<GoodsOutView> list =(List<GoodsOutView>)page.getList();
        for(GoodsOutView c:list){
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
        GoodsOutEntity goodsOut = goodsOutService.selectById(id);
        if(goodsOut !=null){
            //entity转view
            GoodsOutView view = new GoodsOutView();
            BeanUtils.copyProperties( goodsOut , view );//把实体数据重构到view中

            //级联表
            GoodsEntity goods = goodsService.selectById(goodsOut.getGoodsId());
            if(goods != null){
                BeanUtils.copyProperties( goods , view ,new String[]{ "id", "createDate"});//把级联的数据添加到view中,并排除id和创建时间字段
                view.setGoodsId(goods.getId());
            }
            //级联表
            HuiyuanEntity huiyuan = huiyuanService.selectById(goodsOut.getHuiyuanId());
            if(huiyuan != null){
                BeanUtils.copyProperties( huiyuan , view ,new String[]{ "id", "createDate"});//把级联的数据添加到view中,并排除id和创建时间字段
                view.setHuiyuanId(huiyuan.getId());
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
    public R save(@RequestBody GoodsOutEntity goodsOut, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,goodsOut:{}",this.getClass().getName(),goodsOut.toString());
        GoodsEntity goodsEntity = goodsService.selectById(goodsOut.getGoodsId());
        if(goodsEntity == null){
            return R.error(511,"查不到商品");
        }
        Integer i = goodsEntity.getGoodsNumber() - goodsOut.getGoodsOutNumber();
        if(i <0){
            return R.error(511,"出库数量大于库存数量");
        }
        goodsEntity.setGoodsNumber(i);
        goodsService.updateById(goodsEntity);
        Double v = goodsOut.getGoodsOutNumber() * goodsEntity.getDanjia();
        if(goodsOut.getHuiyuanId() != null){
            goodsOut.setGoodsOutMoney(v*0.95);
        }else{
            goodsOut.setGoodsOutMoney(v);
        }
        goodsOut.setInsertTime(new Date());
        goodsOut.setCreateTime(new Date());
        goodsOutService.insert(goodsOut);


        return R.ok();
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody GoodsOutEntity goodsOut, HttpServletRequest request){
        logger.debug("update方法:,,Controller:{},,goodsOut:{}",this.getClass().getName(),goodsOut.toString());
        GoodsOutEntity oldGoodsOutEntity = goodsOutService.selectById(goodsOut.getId());// 查出来原先的订单
        GoodsEntity oldGoodsEntity = goodsService.selectById(oldGoodsOutEntity.getGoodsId());// 原先商品
        if(oldGoodsEntity == null){
            return R.error(511,"查不到原先商品");
        }
        oldGoodsEntity.setGoodsNumber(oldGoodsEntity.getGoodsNumber() + oldGoodsOutEntity.getGoodsOutNumber());//设置原先的商品

        GoodsEntity goodsEntity = goodsService.selectById(goodsOut.getGoodsId());//现在的
        int i = goodsEntity.getGoodsNumber() - goodsOut.getGoodsOutNumber();
        if(i < 0){
            return R.error(511,"修改后的商品的购买数量不能大于库存数量");
        }
        goodsEntity.setGoodsNumber(i);
        Double v = goodsEntity.getDanjia() * goodsOut.getGoodsOutNumber();//算出来的总价
        if(goodsOut.getHuiyuanId() != null){
            goodsOut.setGoodsOutMoney(v*0.95);
        }else{
            goodsOut.setGoodsOutMoney(v);
        }
        goodsService.updateById(oldGoodsEntity);//更新原先商品
        goodsService.updateById(goodsEntity);//更新现在商品
        goodsOutService.updateById(goodsOut);//根据id更新
        return R.ok();
    }



    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        goodsOutService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }






}

