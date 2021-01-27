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
package com.microsoft.azpubsub.security.authenticator;

import java.util.LinkedHashSet;
import java.util.Set;

/*
 * AzPubSub Certificate Identity model
 */
public class CertificateIdentity {
    private String principalName;
    private Set<String> scopes;

    public CertificateIdentity(String principalName) {
        this.principalName = principalName;
        this.scopes = new LinkedHashSet<String>();
    }

    public String principalName() {
        return this.principalName;
    }

    public Set<String> scope() {
        return this.scopes;
    }

    public void addScope(String scope) {
        this.scopes.add(scope);
    }

    @Override
    public String toString() {
        return String.format("Principal Name: %s; Scopes: %s", principalName, scopes);
    }
}
