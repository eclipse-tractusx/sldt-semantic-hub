/********************************************************************************
 * Copyright (c) 2021-2022 Robert Bosch Manufacturing Solutions GmbH
 * Copyright (c) 2021-2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/
package org.eclipse.tractusx.semantics;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.DefaultHttpSecurityExpressionHandler;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;

@Profile("!local")
@Configuration
public class OAuthSecurityConfig {

    private final ApplicationContext applicationContext;

    public OAuthSecurityConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    private WebExpressionAuthorizationManager expressionManager(String expression) {
        var expressionHandler = new DefaultHttpSecurityExpressionHandler();
        expressionHandler.setApplicationContext(applicationContext);
        var manager = new WebExpressionAuthorizationManager(expression);
        manager.setExpressionHandler(expressionHandler);
        return manager;
    }

    @Bean
    protected SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS).permitAll()
                .requestMatchers("/", "/swagger-ui/**", "/swagger-resources/**",
                                  "/v3/api-docs/**", "/semantic-hub-openapi.yaml").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/**/models/**").access(expressionManager("@authorizationEvaluator.hasRoleViewSemanticModel()"))
                .requestMatchers(HttpMethod.POST, "/**/models/**").access(expressionManager("@authorizationEvaluator.hasRoleAddSemanticModel()"))
                .requestMatchers(HttpMethod.PUT, "/**/models/**").access(expressionManager("@authorizationEvaluator.hasRoleUpdateSemanticModel()"))
                .requestMatchers(HttpMethod.DELETE, "/**/models/**").access(expressionManager("@authorizationEvaluator.hasRoleDeleteSemanticModel()")))
            // CSRF protection is disabled because this is a stateless REST API using OAuth2 JWT bearer tokens.
            // CSRF attacks exploit cookie-based authentication; since no session cookies are used, CSRF is not applicable.
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }
}
