package com.minimall.minimall.interceptor;

import com.minimall.minimall.common.util.JwtUtil;
import com.minimall.minimall.common.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Controller 执行前调用 —— 验证 token
     * 返回 true 放行；返回 false 拦截
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // ① 从请求头取 token
        String token = request.getHeader("Authorization");

        // ② token 不能为空
        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            return false;
        }

        // ③ 去掉 "Bearer " 前缀（行业惯例）
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // ④ 解析 token，拿到 userId
        try {
            Long userId = jwtUtil.getUserIdFromToken(token);
            // ⑤ 存进 ThreadLocal，Controller 里能取
            UserContext.setUserId(userId);
            log.info("用户 {} 通过验证", userId);
            return true;
        } catch (Exception e) {
            log.warn("token 验证失败: {}", e.getMessage());
            response.setStatus(401);
            return false;
        }
    }

    /**
     * Controller 执行后调用 —— 清理 ThreadLocal
     * 防止线程被复用时数据串号 + 内存泄漏
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
