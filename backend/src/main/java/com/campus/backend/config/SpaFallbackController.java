package com.campus.backend.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * SPA (Single Page Application) Fallback控制器
 * 
 * Vue Router使用History模式时，直接访问 /login, /products 等路径
 * 需要服务端返回 index.html，由前端路由接管。
 * 
 * 必须使用 @Controller 而非 @RestController，
 * 因为 "forward:/index.html" 需要被Spring MVC作为视图名处理（转发），
 * 而非作为JSON响应体返回。
 */
@Order(Ordered.LOWEST_PRECEDENCE)
@Controller
public class SpaFallbackController {

    @RequestMapping(value = {"/{path:[^\\.]*}"})
    public String fallback() {
        return "forward:/index.html";
    }
}