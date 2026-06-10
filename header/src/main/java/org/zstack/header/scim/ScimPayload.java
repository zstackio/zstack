package org.zstack.header.scim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Generic SCIM payload accepted by the receiver.
 *
 * Resource identity is resolved in this order: request path id, {@link #uuid}, {@link #id}, then {@link #externalId}.
 * Paired *Id and *Uuid fields carry the same logical identity from different upstream naming conventions; concrete
 * resource handlers decide which field maps to their local model.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScimPayload {
    public String id;
    public String uuid;
    public String externalId;
    public String userName;
    public String name;
    public String displayName;
    public String description;
    public String type;
    public Boolean active;
    public Boolean enabled;
    public String parentId;
    public String parentUuid;
    public String organizationId;
    public String organizationUuid;
    public String projectId;
    public String projectUuid;
    public String userId;
    public String userUuid;
    public String virtualIDUuid;
    public String groupId;
    public String groupUuid;
    public String roleId;
    public String roleUuid;
    public String subjectType;
    public String subjectId;
    public String subjectUuid;
    public String principalType;
    public String accountId;
    public String accountUuid;
    public String tenantId;
    public String tenantUuid;
    public String targetAccountId;
    public String targetAccountUuid;
    public String linkAccountUuid;
    public List<ScimMemberRef> members;
    public Map<String, Object> attributes;
}
