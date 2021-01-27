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
package com.microsoft.azpubsub.kafka.admin;

import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import java.util.Objects;

public class AzPubSubAccessControlEntry {

    private final String principal;
    private final String host;
    private final AclOperation operation;
    private final AclPermissionType permissionType;
    private final String aggregatedOperation;

    public AzPubSubAccessControlEntry(String principal, String host, AclOperation operation, AclPermissionType permissionType, String aggregatedOperation) {
        Objects.requireNonNull(principal);
        Objects.requireNonNull(host);
        Objects.requireNonNull(operation);
        Objects.requireNonNull(permissionType);
        if (permissionType == AclPermissionType.ANY)
            throw new IllegalArgumentException("permissionType must not be ANY");
        this.principal = principal;
        this.host = host;
        this.operation = operation;
        this.permissionType = permissionType;
        this.aggregatedOperation = aggregatedOperation;
    }

    public String principal() {
        return principal;
    }

    public String host() {
        return host;
    }

    public AclOperation operation() {
        return operation;
    }

    public AclPermissionType permissionType() {
        return permissionType;
    }

    public String aggregatedOperation() {
        return aggregatedOperation;
    }

    @Override
    public String toString() {
        return "(principal=" + (principal == null ? "<any>" : principal) +
                ", host=" + (host == null ? "<any>" : host) +
                ", operation=" + operation +
                ", permissionType=" + permissionType +
                ", aggregatedOperation=" + aggregatedOperation + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AzPubSubAccessControlEntry))
            return false;
        AzPubSubAccessControlEntry other = (AzPubSubAccessControlEntry) o;
        return Objects.equals(principal, other.principal) &&
                Objects.equals(host, other.host) &&
                Objects.equals(operation, other.operation) &&
                Objects.equals(permissionType, other.permissionType) &&
                Objects.equals(aggregatedOperation, other.aggregatedOperation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(principal, host, operation, permissionType, aggregatedOperation);
    }

}
