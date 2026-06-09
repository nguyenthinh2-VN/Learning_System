package com.example.learning_system_spring.infrastructure.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final PermissionCacheService permissionCacheService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        if (!jwtService.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims = jwtService.parseToken(token);
        String username = claims.getSubject();
        String role = claims.get("role", String.class);

        // Phương án C (Hybrid): JWT chỉ giữ role; permission được nạp động từ cache.
        // Set CẢ ROLE_<role> (giữ tương thích hasRole(...) cũ) LẪN từng permission
        // (cho hasAuthority('PERMISSION') mới) — migrate dần (chốt 2.2).
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (role != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            for (String perm : permissionCacheService.getPermissions(role)) {
                authorities.add(new SimpleGrantedAuthority(perm));
            }
        }

        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(username, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authToken);
        filterChain.doFilter(request, response);
    }
}
