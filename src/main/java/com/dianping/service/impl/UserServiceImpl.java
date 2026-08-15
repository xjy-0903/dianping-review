package com.dianping.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.dto.LoginForm;
import com.dianping.entity.User;
import com.dianping.mapper.UserMapper;
import com.dianping.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User login(LoginForm form) {
        String phone = form.getPhone();
        String md5 = DigestUtils.md5DigestAsHex(form.getPassword().getBytes(StandardCharsets.UTF_8));
        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) {
            user = new User();
            user.setPhone(phone);
            user.setPassword(md5);
            user.setNickName("用户" + phone.substring(phone.length() - 4));
            user.setCreateTime(LocalDateTime.now());
            save(user);
            return user;
        }
        if (!user.getPassword().equals(md5)) {
            throw new IllegalArgumentException("密码错误");
        }
        return user;
    }
}
