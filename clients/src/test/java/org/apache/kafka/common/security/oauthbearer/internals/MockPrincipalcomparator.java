package org.apache.kafka.common.security.oauthbearer.internals;

import java.util.Comparator;

public class MockPrincipalcomparable implements Comparator<String> {

    public int compare(String s1, String s2) {
        return 0;
    }
}
