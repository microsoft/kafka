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
package com.microsoft.azpubsub.security.auth;

import java.util.Set;

import org.apache.kafka.common.security.auth.KafkaPrincipal;

/*
 * AzPubSub Principal holding role
 */
public class AzPubSubPrincipal extends KafkaPrincipal {
    private Set<String> roles;

    public AzPubSubPrincipal(String principalType, String name, Set<String> roles) {
        super(principalType, name);
        this.roles = roles;
    }

    public Set<String> getRoles() {
        return this.roles;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AzPubSubPrincipal)) return false;
        if (!super.equals(o)) return false;
        return roles.equals(((AzPubSubPrincipal) o).roles);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
