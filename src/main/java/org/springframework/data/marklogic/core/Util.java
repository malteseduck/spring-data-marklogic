package org.springframework.data.marklogic.core;

import static org.springframework.util.StringUtils.hasText;

public final class Util {
    private Util() {}

    public static String coalesce(String... candidates) {
        for (String value : candidates) {
            if (hasText(value)) {
                return value;
            }
        }
        throw new IllegalArgumentException("No valid candidate arguments");
    }
}
