package org.zstack.header.image;

import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vo.*;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.Index;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = ImageGroupVO.class, myField = "ImageGroupUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = ImageVO.class, myField = "ImageUuid", targetField = "uuid"),
        }
)
public class ImageGroupRefVO implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    private String imageUuid;

    @Column
    private String imageGroupUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getImageGroupUuid() {
        return imageGroupUuid;
    }

    public void setImageGroupUuid(String imageGroupUuid) {
        this.imageGroupUuid = imageGroupUuid;
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
}