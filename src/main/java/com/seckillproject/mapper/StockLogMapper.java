package com.seckillproject.mapper;

import com.seckillproject.entity.StockLog;

public interface StockLogMapper {

    int deleteByPrimaryKey(String stockLogId);

    int insert(StockLog record);

    int insertSelective(StockLog record);

    StockLog selectByPrimaryKey(String stockLogId);

    int updateByPrimaryKeySelective(StockLog record);

    int updateByPrimaryKey(StockLog record);
}