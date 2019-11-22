package com.leyou.auth.controller;

import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.service.AuthService;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.utils.CookieUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@EnableConfigurationProperties(JwtProperties.class)
public class AuthController {
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private AuthService authService;
    @PostMapping("accredit")
    public ResponseEntity<Void> accredit(@RequestParam("username") String username,
                                         @RequestParam("password") String password,
                                         HttpServletRequest request,
                                         HttpServletResponse response){
        String token = authService.accredit(username,password);
        if (token == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        CookieUtils.setCookie(request,response,jwtProperties.getCookieName(),token,jwtProperties.getExpire()*60);

        return ResponseEntity.ok(null);
    }

    @GetMapping("verify")
    public ResponseEntity<UserInfo> verifyUser(@CookieValue("LY_TOKEN") String token,
                                               HttpServletRequest request,
                                               HttpServletResponse response){
        try {
            UserInfo user = JwtUtils.getInfoFromToken(token, jwtProperties.getPublicKey());
            if (user == null){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            //刷新token 与 cookie
            String s = JwtUtils.generateToken(user, jwtProperties.getPrivateKey(), jwtProperties.getExpire());
            CookieUtils.setCookie(request,response,jwtProperties.getCookieName(),token,jwtProperties.getExpire()*60);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            e.printStackTrace();
        }
         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
