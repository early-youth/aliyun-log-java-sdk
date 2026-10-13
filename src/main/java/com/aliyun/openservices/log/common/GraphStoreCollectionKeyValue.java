package com.aliyun.openservices.log.common;

import java.io.Serializable;

/** Logical GraphStore schema; physical IDs, roles and versions remain service owned. */
public class GraphStoreCollectionKeyValue implements Serializable {
    private static final long serialVersionUID = 1L;
    /** Required nonempty CK field name. */
    private String name;
    /** Required logical string constant; an empty string is allowed. */
    private String value;

    public GraphStoreCollectionKeyValue() { }

    public GraphStoreCollectionKeyValue(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /** Required nonempty CK field name. */
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    /** Required logical string constant; an empty string is allowed. */
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
