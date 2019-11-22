package com.leyou.gateway.filter;

import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.utils.CookieUtils;
import com.leyou.gateway.config.FilterProperties;
import com.leyou.gateway.config.JwtProperties;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Component
@EnableConfigurationProperties({JwtProperties.class,FilterProperties.class})
public class LoginFilter extends ZuulFilter {
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private FilterProperties filterProperties;

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 10;
    }

    @Override
    public boolean shouldFilter() {
        //获取上下文
        RequestContext currentContext = RequestContext.getCurrentContext();
        //通过上下文获取request
        HttpServletRequest request = currentContext.getRequest();
        //获取url
        String requestURL = request.getRequestURL().toString();
        //对白名单放行，即不执行run()
        List<String> allowPaths = filterProperties.getAllowPaths();
        for (String allowpath: allowPaths) {
            if (StringUtils.contains(requestURL,allowpath)){
                return false;
            }
        }


        return true;
    }

    @Override
    public Object run() throws ZuulException {
        //获取上下文
        RequestContext currentContext = RequestContext.getCurrentContext();
        //通过上下文获取request
        HttpServletRequest request = currentContext.getRequest();
        //通过request获得cookievalue（token）
        String token = CookieUtils.getCookieValue(request, jwtProperties.getCookieName());
        //如果token为null,401
        if (token == null){
            currentContext.setSendZuulResponse(false);
            currentContext.setResponseStatusCode(HttpStatus.SC_UNAUTHORIZED);
        }
        try {
            JwtUtils.getInfoFromToken(token,jwtProperties.getPublicKey());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
