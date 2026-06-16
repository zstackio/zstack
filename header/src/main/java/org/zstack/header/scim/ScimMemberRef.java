package org.zstack.header.scim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScimMemberRef {
    public String value;
    public String uuid;
    public String display;
}
