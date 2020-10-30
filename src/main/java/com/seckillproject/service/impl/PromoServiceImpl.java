package com.seckillproject.service.impl;

import com.seckillproject.entity.Promo;
import com.seckillproject.mapper.PromoMapper;
import com.seckillproject.service.ItemService;
import com.seckillproject.service.PromoService;
import com.seckillproject.service.UserService;
import com.seckillproject.service.model.ItemModel;
import com.seckillproject.service.model.PromoModel;
import com.seckillproject.service.model.UserModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Zhang Yifei
 * @date 2019/12/7 14:50
 * @description PromoService的实现类
 */
@Service
public class PromoServiceImpl implements PromoService {

    @Autowired
    private PromoMapper promoMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    /**
     * @param itemId：商品id
     * @return PromoModel：秒杀模型
     * @author wang wei
     * @date 2019/12/10
     * @description 通过商品id查询秒杀信息
     */
    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        Promo promo = promoMapper.selectByItemId(itemId);
        PromoModel promoModel = convertFromEntity(promo);
        if (promoModel == null) {
            return null;
        }
        //判断秒杀活动状态
        if (promoModel.getStartTime().isAfterNow()) {
            promoModel.setStatus(1);
        } else if (promoModel.getEndTime().isBeforeNow()) {
            promoModel.setStatus(3);
        } else {
            promoModel.setStatus(2);
        }
        return promoModel;
    }

    @Override
    public void publishPromo(Integer promoId) {
        //通过活动id获取活动
        Promo promo = promoMapper.selectByPrimaryKey(promoId);
        if (promo.getItemId() == null || promo.getItemId().intValue() == 0) {
            return;
        }
        ItemModel itemModel = itemService.getItemById(promo.getItemId());
        //将库存同步到redis内
        //如果将数据库中的库存同步到Redis的过程中，库存发生了改变也就是发生了售卖(这里没有涉及)，应该采用商品上下架的方式
        //活动开始自动上架，活动没有开始是下架状态
        redisTemplate.opsForValue().set("promo_item_stock_" + itemModel.getId(), itemModel.getStock());

        //将大闸的限制数字设到redis内
        redisTemplate.opsForValue().set("promo_door_count_" + promoId, itemModel.getStock().intValue() * 5);
    }

    @Override
    public String generateSecondKillToken(Integer promoId, Integer itemId, Integer userId) {
        //判断是否库存已售罄，若对应的售罄key存在，则直接返回下单失败
        if (redisTemplate.hasKey("promo_item_stock_invalid_" + itemId)) {
            return null;
        }
        Promo promo = promoMapper.selectByPrimaryKey(promoId);
        //entity->model
        PromoModel promoModel = convertFromEntity(promo);
        if (promoModel == null) {
            return null;
        }
        //判断当前时间是否秒杀活动即将开始或正在进行
        if (promoModel.getStartTime().isAfterNow()) {
            promoModel.setStatus(1);
        } else if (promoModel.getEndTime().isBeforeNow()) {
            promoModel.setStatus(3);
        } else {
            promoModel.setStatus(2);
        }
        //判断活动是否正在进行
        if (promoModel.getStatus().intValue() != 2) {
            return null;
        }
        //判断item信息是否存在
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if (itemModel == null) {
            return null;
        }
        //判断用户信息是否存在
        UserModel userModel = userService.getUserByIdInCache(userId);
        if (userModel == null) {
            return null;
        }
        //获取秒杀大闸的count数量
        long result = redisTemplate.opsForValue().increment("promo_door_count_" + promoId, -1);
        if (result < 0) {
            return null;
        }
        //生成token并且存入redis内并给一个5分钟的有效期
        String token = UUID.randomUUID().toString().replace("-", "");

        redisTemplate.opsForValue().set("promo_token_" + promoId + "_userid_" + userId + "_itemid_" + itemId, token);
        redisTemplate.expire("promo_token_" + promoId + "_userid_" + userId + "_itemid_" + itemId, 5, TimeUnit.MINUTES);

        return token;
    }

    /**
     * @param promo
     * @return PromoModel
     * @author wang wei
     * @date 2019/12/10
     * @description 将promo转换为PromoModel
     */
    private PromoModel convertFromEntity(Promo promo) {
        if (promo == null) {
            return null;
        }
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promo, promoModel);
        promoModel.setPromoItemPrice(new BigDecimal(promo.getPromoItemPrice()));
        promoModel.setStartTime(new DateTime(promo.getStartTime()));
        promoModel.setEndTime(new DateTime(promo.getEndTime()));
        return promoModel;
    }
}
