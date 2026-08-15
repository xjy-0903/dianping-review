package com.dianping.controller;

import com.dianping.dto.Result;
import com.dianping.service.SignService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sign")
public class SignController {

    private final SignService signService;

    public SignController(SignService signService) {
        this.signService = signService;
    }

    @PostMapping
    public Result sign() {
        return signService.sign();
    }

    @GetMapping("/count")
    public Result count() {
        return signService.signCount();
    }

    @GetMapping("/status")
    public Result status() {
        return signService.signStatus();
    }

    @GetMapping("/month")
    public Result month() {
        return signService.monthStatus();
    }
}
