package com.dianping.controller;

import com.dianping.dto.LoginForm;
import com.dianping.dto.Result;
import com.dianping.service.UserService;
import com.dianping.utils.UserHolder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public Result login(@RequestBody @Valid LoginForm form) {
        return Result.ok(userService.login(form));
    }

    @GetMapping("/me")
    public Result me() {
        return Result.ok(UserHolder.get());
    }
}
