/********************************************************************************
 * Copyright (c) 2021-2022 Robert Bosch Manufacturing Solutions GmbH
 * Copyright (c) 2021-2022 Contributors to the Eclipse Foundation
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

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

@Profile("!local")
@Configuration
public class OAuthSecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
          .authorizeRequests(auth -> auth
            .mvcMatchers(HttpMethod.OPTIONS).permitAll()
            .antMatchers(HttpMethod.GET,"/**/models/**").access("@authorizationEvaluator.hasRoleViewSemanticModel()")
            .antMatchers(HttpMethod.POST,"/**/models/**").access("@authorizationEvaluator.hasRoleAddSemanticModel()")
            .antMatchers(HttpMethod.PUT,"/**/models/**").access("@authorizationEvaluator.hasRoleUpdateSemanticModel()")
            .antMatchers(HttpMethod.DELETE,"/**/models/**").access("@authorizationEvaluator.hasRoleDeleteSemanticModel()"))
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
            .oauth2ResourceServer().jwt();
    }
}
