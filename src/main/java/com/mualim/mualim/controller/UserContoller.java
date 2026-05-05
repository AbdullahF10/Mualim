package com.mualim.mualim.controller;

import com.mualim.mualim.service.UserService;
import com.mualim.mualim.vo.UserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/v1/api")
public class UserContoller {

    private final UserService userService;

    @GetMapping(value = "/user/{id}")
    public UserVO getUser(){
        return userVo;
    }

    @PostMapping("users")
    public User createUser(){

    }
}
