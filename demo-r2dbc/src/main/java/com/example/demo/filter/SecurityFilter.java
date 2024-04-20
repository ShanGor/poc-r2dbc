package com.example.demo.filter;

import cn.gzten.security.security.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Order(1)
public class SecurityFilter extends AbstractReactiveSecurityFilter {
    @Override
    public List<AuthRule> getAuthorizationRules() {
        return List.of(new AuthRuleExactMatch("/test-security", AuthRule.ALL_METHODS, AuthRule.RuleType.AUTHENTICATED, null));
    }

    @Override
    public Optional<AuthUser> getAuthUser(ServerHttpRequest request) {
        var token = tryToGetHeader("Authorization", request);
        return Optional.empty();
    }
}
