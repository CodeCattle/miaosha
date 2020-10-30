package com.seckillproject.service.impl;

import com.alibaba.fastjson.JSON;
import com.seckillproject.entity.Item;
import com.seckillproject.entity.ItemStock;
import com.seckillproject.entity.StockLog;
import com.seckillproject.error.BusinessException;
import com.seckillproject.error.EmBusinessError;
import com.seckillproject.mapper.ItemMapper;
import com.seckillproject.mapper.ItemStockMapper;
import com.seckillproject.mapper.StockLogMapper;
import com.seckillproject.mq.MqProducer;
import com.seckillproject.mq.RabbitMqConfig;
import com.seckillproject.service.ItemService;
import com.seckillproject.service.PromoService;
import com.seckillproject.service.model.ItemModel;
import com.seckillproject.service.model.PromoModel;
import com.seckillproject.validator.ValidationResult;
import com.seckillproject.validator.ValidatorImpl;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author wang wei
 * @date 2019/12/6 14:12
 * @description ItemService的实现类
 */
@Service
public class ItemServiceImpl implements ItemService{

    @Autowired
    private ValidatorImpl validator;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private ItemStockMapper itemStockMapper;

    @Autowired
    private PromoService promoService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitMqConfig rabbitMqConfig;

    @Autowired
    private StockLogMapper stockLogMapper;

    /**
     * @param itemModel:商品模型
     * @return ItemModel:商品模型
     * @author wang wei
     * @date 2019/12/10
     * @description 创建商品
     */
    @Override
    @Transactional
    public ItemModel createItem(ItemModel itemModel) throws BusinessException {
        //校验入参
        ValidationResult result = validator.validate(itemModel);
        if (result.isHasErrors()) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, result.getErrMsg());
        }
        //转化model
        Item item = this.convertItemFromItemModel(itemModel);
        //写入数据库
        itemMapper.insertSelective(item);
        //因为要关联需要重新设置id
        itemModel.setId(item.getId());
        ItemStock itemStock = this.convertItemStockFromItemModel(itemModel);
        itemStockMapper.insertSelective(itemStock);
        //返回对象
        return this.getItemById(itemModel.getId());
    }
    /**
     * @param itemModel
     * @return Item
     * @author wang wei
     * @date 2019/12/10
     * @description 将itemModel转换为Item
     */
    private Item convertItemFromItemModel(ItemModel itemModel) {
        if (itemModel == null) {
            return null;
        }
        Item item = new Item();
        BeanUtils.copyProperties(itemModel, item);
        //数据库中price是double类型的，ItemModel中是BigDecimal，避免类型转化时出现精度丢失
        item.setPrice(itemModel.getPrice().doubleValue());
        return item;
    }
    /**
     * @param itemModel
     * @return ItemStock：商品库存
     * @author wang wei
     * @date 2019/12/10
     * @description 将itemModel转化为ItemStock
     */
    private ItemStock convertItemStockFromItemModel(ItemModel itemModel) {
        if (itemModel == null) {
            return null;
        }
        ItemStock itemStock = new ItemStock();
        itemStock.setItemId(itemModel.getId());
        itemStock.setStock(itemModel.getStock());
        return itemStock;
    }
    /**
     * @param id：商品id
     * @return ItemModel
     * @author wang wei
     * @date 2019/12/10
     * @description 通过id获取商品详情
     */
    @Override
    public ItemModel getItemById(Integer id) {
        Item item = itemMapper.selectByPrimaryKey(id);
        if (item == null) {
            return null;
        }
        //通过id来获取库存对象
        ItemStock itemStock = itemStockMapper.selectByItemId(item.getId());
        ItemModel itemModel = convertFromEntity(item, itemStock);

        PromoModel promoModel = promoService.getPromoByItemId(itemModel.getId());
        if (promoModel != null && promoModel.getStatus().intValue() != 3) {
            itemModel.setPromoModel(promoModel);
        }
        return itemModel;
    }

    @Override
    public ItemModel getItemByIdInCache(Integer id) {
        ItemModel itemModel = (ItemModel) redisTemplate.opsForValue().get("item_validate_"+id);
        if(itemModel == null){
            itemModel = this.getItemById(id);
            redisTemplate.opsForValue().set("item_validate_"+id,itemModel);
            redisTemplate.expire("item_validate_"+id,10, TimeUnit.MINUTES);
        }
        return itemModel;
    }
    /**
     * @param item
     * @param itemStock
     * @return ItemModel
     * @author wang wei
     * @date 2019/12/10
     * @description 将item和itemStock转换为ItemModel
     */
    private ItemModel convertFromEntity(Item item, ItemStock itemStock) {
        if (item == null) {
            return null;
        }
        ItemModel itemModel = new ItemModel();
        BeanUtils.copyProperties(item, itemModel);

        //从数据库data向item model转的时候也要注意price的类型问题
        itemModel.setPrice(new BigDecimal(item.getPrice()));
        itemModel.setStock(itemStock.getStock());

        return itemModel;
    }

    /**
     * @return List<ItemModel>:商品List
     * @author wang wei
     * @date 2019/12/10
     * @description 列出商品
     */
    @Override
    public List<ItemModel> listItem() {
        List<Item> itemList = itemMapper.listItem();
        List<ItemModel> itemModelList = itemList.stream().map(item -> {
            ItemStock itemStock = itemStockMapper.selectByItemId(item.getId());
            ItemModel itemModel = this.convertFromEntity(item, itemStock);
            return itemModel;
        }).collect(Collectors.toList());
        return itemModelList;
    }

    @Override
    public boolean sendItemStockFromMq(Integer id, Integer stockAmount) {
        Map<String,Object> msg = new HashMap<>();
        msg.put("itemId",id);
        msg.put("amount",stockAmount);
        String jsonString = JSON.toJSONString(msg);
        rabbitTemplate.convertAndSend(MqProducer.EX_ROUTING_ITEM_STOCK,"routingKey_item_stock",msg);
        Map<String,Object> map = RabbitMqConfig.map;
        return (Boolean) map.get("msgStatus");
    }

    @Override
    public boolean increaseStock(Integer id, Integer stockAmount) {
        redisTemplate.opsForValue().increment("promo_item_stock_"+id,stockAmount.intValue());
        return true;
    }

    @Override
    public boolean asyncDecreaseStock(Integer id, Integer stockAmount) {
        sendItemStockFromMq(id,stockAmount);
        Map<String,Object> map = RabbitMqConfig.map;
        return (Boolean) map.get("msgStatus");
    }

    /**@description 扣减库存**/
    @Override
    @Transactional
    public boolean decreaseStock(Integer itemId, Integer amount) throws BusinessException {
        //int affectedRow =  itemStockDOMapper.decreaseStock(itemId,amount);
        long result = redisTemplate.opsForValue().increment("promo_item_stock_"+itemId,amount.intValue() * -1);
        if(result > 0){
            return true;
        }else if(result == 0){
            //打上库存已售罄的标识
            redisTemplate.opsForValue().set("promo_item_stock_invalid_"+itemId,"true");
            //更新库存成功
            return true;
        }else{
            //更新库存失败
            increaseStock(itemId,amount);
            return false;
        }
    }
    //初始化对应的库存流水
    @Override
    @Transactional
    public String initStockLog(Integer itemId, Integer amount) {
        StockLog stockLogDO = new StockLog();
        stockLogDO.setItemId(itemId);
        stockLogDO.setAmount(amount);
        stockLogDO.setStockLogId(UUID.randomUUID().toString().replace("-",""));
        //1表示初始状态，2表示下单扣除库存成功，3表示下单失败
        stockLogDO.setStatus(1);
        stockLogMapper.insertSelective(stockLogDO);
        return stockLogDO.getStockLogId();
    }

    /**
     * @param
     * @return
     * @author wang wei
     * @date 2019/12/10
     * @description 增加商品销量
     */
    @Override
    @Transactional
    public void increaseSales(Integer itemId, Integer amount) throws BusinessException {
        itemMapper.increaseSales(itemId, amount);
    }
}
