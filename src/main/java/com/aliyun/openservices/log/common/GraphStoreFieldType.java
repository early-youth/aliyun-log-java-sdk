package com.aliyun.openservices.log.common;

/** Supported logical schema values; unknown values require an SDK upgrade. */
public enum GraphStoreFieldType {
    STRING, INT, DOUBLE, BOOL;

    public String getValue() { return name(); }
}
