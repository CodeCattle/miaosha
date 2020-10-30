package com.seckillproject.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.seckillproject.service.CacheService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Service
public class CacheServiceImpl implements CacheService{
    private Cache<String,Object> commonCache = null;

    //Bean优先执行 init方法
    @PostConstruct
    public void init(){
        commonCache = CacheBuilder.newBuilder()
                //设置缓存容器的初始容量容器
                .initialCapacity(10)
                //设置缓存容器中可以存储的最大数量key，超过100之后按照LRU策略进行移除操作
                .maximumSize(100)
                //设置写缓存过期时期(不可以设置访问过期时间)
                .expireAfterWrite(60, TimeUnit.SECONDS).build();
    }
    @Override
    public void setCommonCache(String key, Object value) {
        commonCache.put(key,value);
    }

    @Override
    public Object getFromCommonCache(String key) {
        return commonCache.getIfPresent(key);
    }
}
