package com.imooc.miaoshaproject.service.impl;

import com.imooc.miaoshaproject.dao.PromoDOMapper;
import com.imooc.miaoshaproject.dataobject.PromoDO;
import com.imooc.miaoshaproject.service.ItemService;
import com.imooc.miaoshaproject.service.PromoService;
import com.imooc.miaoshaproject.service.model.ItemModel;
import com.imooc.miaoshaproject.service.model.PromoModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Created by hzllb on 2018/11/18.
 */
@Service
public class PromoServiceImpl implements PromoService {

    @Autowired
    private PromoDOMapper promoDOMapper;

    @Autowired
    ItemService itemService;

    @Autowired
    RedisTemplate redisTemplate;

    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        //获取对应商品的秒杀活动信息
        PromoDO promoDO = promoDOMapper.selectByItemId(itemId);

        //dataobject->model
        PromoModel promoModel = convertFromDataObject(promoDO);
        if (promoModel == null) {
            return null;
        }

        //判断当前时间是否秒杀活动即将开始或正在进行
        if (new DateTime(promoModel.getStartDate()).isAfterNow()) {
            promoModel.setStatus(1);
        } else if (new DateTime(promoModel.getEndDate()).isBeforeNow()) {
            promoModel.setStatus(3);
        } else {
            promoModel.setStatus(2);
        }
        return promoModel;
    }

    @Override
    public void publishPromo(Integer promoId) {
        // 通过活动id 获取多动
        PromoDO promoDO = promoDOMapper.selectByItemId(promoId);
        if (promoDO == null || promoDO.getId() == 0) {
            return;
        }
        ItemModel itemModel = itemService.getItemById(promoDO.getItemId());
        // 库存同步到redis内
        redisTemplate.opsForValue().set("promo_item_stock_" + itemModel.getId(), itemModel.getStock());
    }

    private PromoModel convertFromDataObject(PromoDO promoDO) {
        if (promoDO == null) {
            return null;
        }
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promoDO, promoModel);
        promoModel.setPromoItemPrice(new BigDecimal(promoDO.getPromoItemPrice()));
        promoModel.setStartDate(promoDO.getStartDate());
        promoModel.setEndDate(promoDO.getEndDate());
        return promoModel;
    }
}
