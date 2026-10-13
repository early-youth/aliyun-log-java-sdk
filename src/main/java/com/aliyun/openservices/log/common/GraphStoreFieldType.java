package com.aliyun.openservices.log.common;

/** Supported logical schema values; unknown values require an SDK upgrade. */
public enum GraphStoreFieldType {
    STRING("STRING"), INT("INT"), DOUBLE("DOUBLE"), BOOL("BOOL");

    private final String value;
    GraphStoreFieldType(String value) { this.value = value; }
    public String getValue() { return value; }
}
