package com.seckillproject.service;

import com.seckillproject.error.BusinessException;
import com.seckillproject.service.model.ItemModel;

import java.util.List;

/**
 * @author wang wei
 * @date 2019/12/6 14:12
 */
public interface ItemService {

    /**
     * @author wang wei
     * @date 2019/12/10
     * @param itemModel:商品模型
     * @return ItemModel:商品模型
     * @description 创建商品
     */
    ItemModel createItem(ItemModel itemModel) throws BusinessException;

    /**
     * @author wang wei
     * @date 2019/12/10
     * @param
     * @return List<ItemModel>:商品List
     * @description 列出商品
     */
    List<ItemModel> listItem();

    /**
     * @author wang wei
     * @date 2019/12/10
     * @param id：商品id
     * @return ItemModel
     * @description 通过id获取商品详情
     */
    ItemModel getItemById(Integer id);

    //item及promo model缓存模型
    ItemModel getItemByIdInCache(Integer id);

    //扣除库存消息
    boolean sendItemStockFromMq(Integer id,Integer stockAmount);

    //回滚 redis(库存)操作
    boolean increaseStock(Integer id,Integer stockAmount);

   //异步更新库存
    boolean asyncDecreaseStock(Integer id,Integer stockAmount);

    //初始化库存流水
    String initStockLog(Integer itemId,Integer amount) throws BusinessException;

   /**
     * @author wang wei
     * @date 2019/12/10
     * @param itemId
     * @param amount
     * @return boolean
     * @description 扣减库存
     */
    boolean decreaseStock(Integer itemId,Integer amount) throws BusinessException;

    /**
     * @author wang wei
     * @date 2019/12/10
     * @param
     * @return
     * @description 增加商品销量
     */
    void increaseSales(Integer itemId,Integer amount) throws BusinessException;

}
