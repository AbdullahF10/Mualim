package com.mualim.mualim.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
public class UserController {

    @GetMapping(path = "/user")
    public String User(){
        return "hi ";
    }
}
