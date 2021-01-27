/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microsoft.azpubsub.security.oauthbearer;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.kafka.common.security.oauthbearer.OAuthBearerToken;

/*
 * AzPubSub oAuth token model
 */
public class AzPubSubOAuthBearerToken implements OAuthBearerToken {
    private String value;
    private Set<String> scopes;
    private long lifetimeMs;
    private String principalName;
    private Long startTimeMs;

    public AzPubSubOAuthBearerToken(String accessToken, long lifetimeS, String principalName, Long startTimeMs) {
        super();
        this.value = accessToken;
        this.scopes = new LinkedHashSet<String>();
        this.lifetimeMs = lifetimeS;
        this.principalName = principalName;
        this.startTimeMs = startTimeMs;
    }

    @Override
    public String value() {
        return this.value;
    }

    @Override
    public Set<String> scope() {
        return this.scopes;
    }

    @Override
    public long lifetimeMs() {
        return this.lifetimeMs;
    }

    @Override
    public String principalName() {
        return this.principalName;
    }

    @Override
    public Long startTimeMs() {
        return this.startTimeMs;
    }

    public void addScope(String scope) {
        this.scopes.add(scope);
    }
}
