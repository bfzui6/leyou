package com.leyou.user.service;

import com.leyou.common.utils.NumberUtils;
import com.leyou.user.mapper.UserMapper;
import com.leyou.user.pojo.User;
import com.leyou.user.utils.CodecUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AmqpTemplate amqpTemplate;
    @Autowired
    private StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "user:code:phone:";

    public Boolean check(String data, Integer type) {
        User user = new User();
        if (type == 1){
            user.setUsername(data);
        }else if (type == 2){
            user.setPhone(data);
        }else {
            return null;
        }
        return  userMapper.selectCount(user) == 0;
    }

    public void sendVerifyCode(String phone) {
        //创建验证码
        String s = NumberUtils.generateCode(6);
        Map<String,String> map = new HashMap<>();
        map.put("phone",phone);
        map.put("code",s);

        //发送短信
        amqpTemplate.convertAndSend("leyou.sms.exchange","sms.verify.code",map);
        //保存验证码到redis中
        redisTemplate.opsForValue().set(KEY_PREFIX+phone,s,5, TimeUnit.MINUTES);
    }

    public void register(User user, String code) {
        //校验验证码
        String s = redisTemplate.opsForValue().get(KEY_PREFIX + user.getPhone());
        if (!s.equals(code)){
            return;
        }

        //生成盐  并且对密码加密
        String salt = CodecUtils.generateSalt();
        user.setPassword(CodecUtils.md5Hex(user.getPassword(),salt));

        //封装user
        user.setCreated(new Date());
        user.setSalt(salt);
        user.setId(null);
        //添加到数据库
        userMapper.insertSelective(user);
    }

    public User query(String username, String password) {
        //首先根据用户名来查出user
        User user1 = new User();
        user1.setUsername(username);
        User user = userMapper.selectOne(user1);
        if (user == null){
            return null;
        }
        //拿user的盐对接收到的password加密得到加密后的 password
        password = CodecUtils.md5Hex(password, user.getSalt());
        //将加密后的password与user的password对比  如果一致接收到的password便没有问题
        if (StringUtils.equals(password,user.getPassword())){
            return user;
        }
        return null;
    }
}
