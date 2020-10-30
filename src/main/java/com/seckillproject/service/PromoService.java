package com.seckillproject.service;

import com.seckillproject.service.model.PromoModel;

/**
 * @author Zhang Yifei
 * @date 2019/12/7 14:48
 */
public interface PromoService {
    /**
     * @author wang wei
     * @date 2019/12/10
     * @param itemId 商品id
     * @return PromoModel 秒杀模型
     * @description 通过商品id查询秒杀信息
     */
    PromoModel getPromoByItemId(Integer itemId);

    //活动发布,将库存放入到缓存中
    void publishPromo(Integer promoId);

    //生成秒杀令牌
    public String generateSecondKillToken(Integer promoId,Integer itemId,Integer userId);
}
