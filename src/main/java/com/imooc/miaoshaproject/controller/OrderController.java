package com.imooc.miaoshaproject.controller;

import com.imooc.miaoshaproject.dataobject.StockLogDO;
import com.imooc.miaoshaproject.error.BusinessException;
import com.imooc.miaoshaproject.error.EmBusinessError;
import com.imooc.miaoshaproject.mq.MqProducer;
import com.imooc.miaoshaproject.response.CommonReturnType;
import com.imooc.miaoshaproject.service.ItemService;
import com.imooc.miaoshaproject.service.OrderService;
import com.imooc.miaoshaproject.service.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by hzllb on 2018/11/18.
 */
@Controller
@RequestMapping("/order")
@CrossOrigin(origins = {"*"}, allowCredentials = "true")
public class OrderController extends BaseController {
    @Autowired
    private OrderService orderService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    ItemService itemService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    MqProducer mqProducer;


    //封装下单请求
    @RequestMapping(value = "/createorder", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam(name = "itemId") Integer itemId,
                                        @RequestParam(name = "amount") Integer amount,
                                        @RequestParam(name = "promoId", required = false) Integer promoId) throws BusinessException {

        //Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户还未登陆，不能下单");
        }
        //获取用户的登陆信息
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if (userModel == null) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户还未登陆，不能下单");
        }


        //UserModel userModel = (UserModel)httpServletRequest.getSession().getAttribute("LOGIN_USER");

//        OrderModel orderModel = orderService.createOrder(userModel.getId(),itemId,promoId,amount);

        // 加入库存流水init状态
        StockLogDO stockLogDO = itemService.initStockLog(itemId, amount);

        if (!mqProducer.transactionAsyncReduceStock(stockLogDO.getStockLogId(), userModel.getId(), promoId, itemId, amount)) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR, "下单失败");
        }
        return CommonReturnType.create(null);
    }
}
