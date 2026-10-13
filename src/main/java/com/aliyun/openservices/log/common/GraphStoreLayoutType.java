package com.aliyun.openservices.log.common;

/** Supported logical schema values; unknown values require an SDK upgrade. */
public enum GraphStoreLayoutType {
    VERTEX("vertex"), EDGE("edge");

    private final String value;
    GraphStoreLayoutType(String value) { this.value = value; }
    public String getValue() { return value; }
}
