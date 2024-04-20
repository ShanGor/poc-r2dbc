package com.example.demo.filter;

import cn.gzten.security.security.AuthRule;
import cn.gzten.security.security.AuthRuleExactMatch;
import cn.gzten.security.security.AuthUser;
import cn.gzten.security.security.AbstractSecurityFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;
import java.util.Optional;

@Slf4j
@Configuration
@Order(1)
public class DefaultSecurityFilter extends AbstractSecurityFilter {
    public List<AuthRule> getAuthorizationRules() {
        return List.of(new AuthRuleExactMatch("/test", AuthRule.ALL_METHODS, AuthRule.RuleType.ANONYMOUS, null));
    }

    public Optional<AuthUser> getAuthUser(HttpServletRequest request) {
        var user = new AuthUser();
        user.setUsername("samuel");
        user.addRole("admin");
        return Optional.of(user);
    }
}
