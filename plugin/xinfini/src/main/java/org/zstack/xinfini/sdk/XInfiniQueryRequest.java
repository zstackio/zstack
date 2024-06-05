package org.zstack.xinfini.sdk;

/**
 * {"q": "((spec.name:~\".*1.*\") AND (spec.description:~\".*1.*\"))",
 * "sort": "spec.created_at:asc",
 * "limit": 100,
 * "offset": 0}
 */
public abstract class XInfiniQueryRequest extends XInfiniRequest {
    public String q;
    public Long limit;
    public Long offset;
    public String sortBy;
    public String metric;
    public String lables;
}
