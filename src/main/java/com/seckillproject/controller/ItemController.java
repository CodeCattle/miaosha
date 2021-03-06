package com.seckillproject.controller;

import com.seckillproject.controller.viewobject.ItemVO;
import com.seckillproject.error.BusinessException;
import com.seckillproject.response.CommonReturnType;
import com.seckillproject.service.CacheService;
import com.seckillproject.service.ItemService;
import com.seckillproject.service.PromoService;
import com.seckillproject.service.model.ItemModel;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author wang wei
 * @date 2019/12/6 15:20
 * @description 处理前端对商品的请求
 */
@RestController
@RequestMapping("/item")
@CrossOrigin(allowCredentials = "true",allowedHeaders = "*")
public class ItemController extends BaseController {

    @Autowired
    private ItemService itemService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private PromoService promoService;

    /**
     * @author wang wei
     * @date 2019/12/10
     * @param  [ title：商品标题, price：价格, description：描述, imgUrl：图片链接, stock：库存]
     * @return CommonReturnType:通用返回类型
     * @description 创建商品
     */
    @PostMapping(value = "/create", consumes = CONTENT_TYPE_FORMED)
    @ResponseBody
    public CommonReturnType createItem(@RequestParam(name = "title") String title,
                                       @RequestParam(name = "price") BigDecimal price,
                                       @RequestParam(name = "description") String description,
                                       @RequestParam(name = "imgUrl") String imgUrl,
                                       @RequestParam(name = "stock") Integer stock) throws BusinessException {
        ItemModel itemModel = new ItemModel();
        itemModel.setTitle(title);
        itemModel.setPrice(price);
        itemModel.setDescription(description);
        itemModel.setImgUrl(imgUrl);
        itemModel.setStock(stock);

        ItemModel itemModelForReturn = itemService.createItem(itemModel);
        ItemVO itemVO = convertVOFromModel(itemModelForReturn);
        return CommonReturnType.create(itemVO);
    }

    /**
     * @author wang wei
     * @date 2019/12/10
     * @param id
     * @return CommonReturnType
     * @description 获取商品详情
     */
    @GetMapping(value = "/get")
    @ResponseBody
    public CommonReturnType getItem(@RequestParam(name = "id") Integer id) {
        ItemModel itemModel = null;
        //先从本地缓存中获取
        itemModel = (ItemModel) cacheService.getFromCommonCache("item_"+id);
        if(itemModel == null){
            //本地没有则从redis缓存进行获取
            itemModel = (ItemModel)redisTemplate.opsForValue().get("item_" + id);
            if (itemModel == null){
                //Redis缓存中没有则调用Service
                itemModel = itemService.getItemById(id);
                //调用Service之后将itemModel放入redis中
                redisTemplate.opsForValue().set("item_"+id,itemModel);
                redisTemplate.expire("item_"+id,10, TimeUnit.MINUTES);
            }
            //将获得的itemModel放入本地缓存
            cacheService.setCommonCache("item_"+id,itemModel);
        }
        ItemVO itemVO = this.convertVOFromModel(itemModel);
        return CommonReturnType.create(itemVO);
    }

    /**
     * @author wang wei
     * @date 2019/12/10
     * @return CommonReturnType
     * @description 获取商品列表
     */
    @GetMapping(value = "/list")
    @ResponseBody
    public CommonReturnType listItem() {
        List<ItemModel> itemModelList = itemService.listItem();
        List<ItemVO> itemVOList = itemModelList.stream().map(itemModel -> {
            ItemVO itemVO = this.convertVOFromModel(itemModel);
            return itemVO;
        }).collect(Collectors.toList());
        return CommonReturnType.create(itemVOList);
    }

    /**
     * @author wang wei
     * @date 2019/12/10
     * @param itemModel
     * @return ItemVO
     * @description 将itemModel转换为ItemVO
     */
    private ItemVO convertVOFromModel(ItemModel itemModel) {
        if (itemModel == null) {
            return null;
        }
        ItemVO itemVO = new ItemVO();
        BeanUtils.copyProperties(itemModel, itemVO);
        if (itemModel.getPromoModel() != null) {
            itemVO.setPromoStatus(itemModel.getPromoModel().getStatus());
            itemVO.setPromoId(itemModel.getPromoModel().getId());
            itemVO.setPromoPrice(itemModel.getPromoModel().getPromoItemPrice());
            itemVO.setStartTime(itemModel.getPromoModel().getStartTime().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
        }else {
            itemVO.setPromoStatus(0);
        }

        return itemVO;
    }
    @RequestMapping(value = "/publishpromo",method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType publishpromo(@RequestParam(name = "id")Integer id){
        promoService.publishPromo(id);
        return CommonReturnType.create(null);

    }
}
