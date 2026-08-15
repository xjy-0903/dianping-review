package com.dianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dianping.dto.LoginForm;
import com.dianping.entity.User;

public interface UserService extends IService<User> {

    /**
     * 登录(演示项目: 不存在则自动注册)
     */
    User login(LoginForm form);
}
