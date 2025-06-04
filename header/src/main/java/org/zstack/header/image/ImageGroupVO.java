package org.zstack.header.image;

import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.Column;
import java.sql.Timestamp;
import javax.persistence.*;

@Entity
@Table
@BaseResource
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = ImageGroupRefVO.class, myField = "uuid", targetField = "imageGroupUuid")
        }
)
public class ImageGroupVO extends ResourceVO {
    @Column
    private Integer imageCount;
    @Column
    private String name;
    @Column
    @Enumerated(EnumType.STRING)
    private ImageStatus status;
    @Column
    private String description;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public Integer getImageCount() {
        return imageCount;
    }

    public void setImageCount(Integer imageCount) {
        this.imageCount = imageCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public ImageStatus getStatus() {
        return status;
    }

    public void setStatus(ImageStatus status) {
        this.status = status;
    }
}