package org.zstack.network.service.header.acl;

import org.apache.commons.net.ntp.TimeStamp;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-05
 **/
@Entity
@Table
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = AccessControlListVO.class, myField = "aclUuid", targetField = "uuid"),
        }
)
public class AccessControlListEntryVO implements ToInventory {
    @Id
    @Column
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long entryId;

    @Column
    @ForeignKey(parentEntityClass = AccessControlListVO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.CASCADE)
    private String aclUuid;

    @Column
    private String ipEntries;
    @Column
    private String description;
    @Column
    private TimeStamp createDate;
    @Column
    private TimeStamp lastOpDate;

    public Long getEntryId() {
        return entryId;
    }

    public void setEntryId(Long entryId) {
        this.entryId = entryId;
    }

    public String getAclUuid() {
        return aclUuid;
    }

    public void setAclUuid(String aclUuid) {
        this.aclUuid = aclUuid;
    }

    public String getIpEntries() {
        return ipEntries;
    }

    public void setIpEntries(String ipEntries) {
        this.ipEntries = ipEntries;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TimeStamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(TimeStamp createDate) {
        this.createDate = createDate;
    }

    public TimeStamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(TimeStamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
