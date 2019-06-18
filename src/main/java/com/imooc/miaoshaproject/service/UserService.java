package com.imooc.miaoshaproject.service;

import com.imooc.miaoshaproject.error.BusinessException;
import com.imooc.miaoshaproject.service.model.UserModel;

/**
 * Created by hzllb on 2018/11/11.
 */
public interface UserService {
    //通过用户ID获取用户对象的方法
    UserModel getUserById(Integer id);

    /**
     * 从缓存中取用户信息
     *
     * @param userId 用户id
     * @return com.imooc.miaoshaproject.service.model.UserModel
     * @author hyf
     * @date 2019-06-18
     */
    UserModel getUserByIdInCache(Integer userId);

    void register(UserModel userModel) throws BusinessException;

    /*
    telphone:用户注册手机
    password:用户加密后的密码
     */
    UserModel validateLogin(String telphone, String encrptPassword) throws BusinessException;
}
