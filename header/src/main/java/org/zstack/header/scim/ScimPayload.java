package org.zstack.header.scim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Generic SCIM payload accepted by the receiver.
 *
 * Resource identity is resolved from the request path UUID or {@link #uuid}. The receiver intentionally does not
 * use SCIM id/externalId aliases because ZIAM owns stable UUID generation before publishing downstream events.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScimPayload {
    public String uuid;
    public String userName;
    public String name;
    public String displayName;
    public String description;
    public String type;
    public Boolean active;
    public Boolean enabled;
    public String parentUuid;
    public String organizationUuid;
    public String projectUuid;
    public String userUuid;
    public String virtualIDUuid;
    public String groupUuid;
    public String roleUuid;
    public String templateRoleUuid;
    public String externalType;
    public String subjectType;
    public String subjectUuid;
    public String principalType;
    public String accountUuid;
    public String tenantUuid;
    public String targetAccountUuid;
    public String linkAccountUuid;
    public String scopeUuid;
    public List<ScimMemberRef> members;
    public Map<String, Object> attributes;
}
