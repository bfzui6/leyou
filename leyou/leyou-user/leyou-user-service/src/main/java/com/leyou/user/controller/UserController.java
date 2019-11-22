package com.leyou.user.controller;

import com.leyou.user.pojo.User;
import com.leyou.user.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Controller
@RequestMapping
public class UserController {
    @Autowired
    private UserService userService;
    @GetMapping("/check/{data}/{type}")
    public ResponseEntity<Boolean> checkUser(@PathVariable("data") String data,@PathVariable("type") Integer type){

        Boolean b = userService.check(data,type);
        if (b == null){
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(b);
    }
    @PostMapping("code")
    public ResponseEntity<Void> sendVerifyCode(String phone) {
        if (StringUtils.isBlank(phone)){
            return ResponseEntity.badRequest().build();
        }
        userService.sendVerifyCode(phone);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    @PostMapping("register")
    public ResponseEntity<Void> register(@Valid User user, @RequestParam("code") String code){
        userService.register(user,code);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("query")
    public ResponseEntity<User> query(@RequestParam("username")String username,@RequestParam("password")String password){
        User user = userService.query(username,password);
        if (user == null){
            return ResponseEntity.badRequest().build();
        }
       return ResponseEntity.ok(user);
    }

}
