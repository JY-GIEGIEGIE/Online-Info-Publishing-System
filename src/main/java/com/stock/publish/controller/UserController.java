package com.stock.publish.controller;

import com.stock.publish.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/upgrade")
    public Object upgrade() {
        // TODO: POST /api/publish/user/upgrade
        // 权限：STANDARD，模拟支付后更新 is_premium = true
        throw new UnsupportedOperationException("TODO");
    }
}
