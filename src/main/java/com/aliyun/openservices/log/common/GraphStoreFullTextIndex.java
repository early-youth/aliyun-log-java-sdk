package com.aliyun.openservices.log.common;

import java.io.Serializable;

/** Logical GraphStore schema; physical IDs, roles and versions remain service owned. */
public class GraphStoreFullTextIndex implements Serializable {
    private static final long serialVersionUID = 1L;
    /** Required nonempty token characters. */
    private String token;
    /** Optional nonempty subset of token characters; null omits the field. */
    private String optionToken;
    /** Required case sensitivity setting. */
    private Boolean caseSensitive;
    /** Required Chinese tokenization setting. */
    private Boolean chn;

    public GraphStoreFullTextIndex() { }

    public GraphStoreFullTextIndex(String token, Boolean caseSensitive, Boolean chn) {
        this.token = token;
        this.caseSensitive = caseSensitive;
        this.chn = chn;
    }

    /** Required nonempty token characters. */
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    /** Optional nonempty subset of token characters; null omits the field. */
    public String getOptionToken() { return optionToken; }
    public void setOptionToken(String optionToken) { this.optionToken = optionToken; }

    /** Required case sensitivity setting. */
    public Boolean getCaseSensitive() { return caseSensitive; }
    public void setCaseSensitive(Boolean caseSensitive) { this.caseSensitive = caseSensitive; }

    /** Required Chinese tokenization setting. */
    public Boolean getChn() { return chn; }
    public void setChn(Boolean chn) { this.chn = chn; }
}
