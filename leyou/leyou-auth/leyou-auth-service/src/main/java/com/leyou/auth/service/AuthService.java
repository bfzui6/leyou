package com.leyou.auth.service;

import com.leyou.auth.client.UserClient;
import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.user.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@EnableConfigurationProperties(JwtProperties.class)
public class AuthService {
    @Autowired
    private UserClient userClient;
    @Autowired
    private JwtProperties jwtProperties;
    
    public String accredit(String username, String password) {
        //校验用户是否存在
        User query = userClient.query(username, password);
        if (query == null){
            return null;
        }

        //封装userinfo并且声称token
        UserInfo userInfo = new UserInfo();
        userInfo.setId(query.getId());
        userInfo.setUsername(query.getUsername());
        try {
            String token = JwtUtils.generateToken(userInfo, jwtProperties.getPrivateKey(), jwtProperties.getExpire());
            return token;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
