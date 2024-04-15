package org.zstack.header.vm;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;

@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = TemplateVmInstanceVO.class, myField = "templateVmInstanceUuid", targetField = "uuid")
        }
)
public class TemplateVmInstanceCacheVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = TemplateVmInstanceVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String templateVmInstanceUuid;

    @Column
    private String cacheVmInstanceUuid;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTemplateVmInstanceUuid() {
        return templateVmInstanceUuid;
    }

    public void setTemplateVmInstanceUuid(String templateVmInstanceUuid) {
        this.templateVmInstanceUuid = templateVmInstanceUuid;
    }

    public String getCacheVmInstanceUuid() {
        return cacheVmInstanceUuid;
    }

    public void setCacheVmInstanceUuid(String cacheVmInstanceUuid) {
        this.cacheVmInstanceUuid = cacheVmInstanceUuid;
    }
}
