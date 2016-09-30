package org.zstack.storage.fusionstor.primary;

import org.zstack.header.storage.primary.PrimaryStorageEO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.SoftDeletionCascade;
import org.zstack.header.vo.SoftDeletionCascades;
import org.zstack.storage.fusionstor.FusionstorMonAO;

import javax.persistence.*;

/**
 * Created by frank on 7/28/2015.
 */
@Entity
@Table
@Inheritance(strategy= InheritanceType.JOINED)
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = PrimaryStorageVO.class, joinColumn = "primaryStorageUuid")
})
public class FusionstorPrimaryStorageMonVO extends FusionstorMonAO {
    @Column
    @ForeignKey(parentEntityClass = PrimaryStorageEO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.CASCADE)
    private String primaryStorageUuid;

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
}
