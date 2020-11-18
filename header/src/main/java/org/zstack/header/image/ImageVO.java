package org.zstack.header.image;

import org.hibernate.search.annotations.Indexed;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.NoView;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@EO(EOClazz = ImageEO.class)
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = ImageBackupStorageRefVO.class, myField = "uuid", targetField = "imageUuid")
        }
)
@Indexed
public class ImageVO extends ImageAO implements OwnedByAccount {
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "imageUuid", insertable = false, updatable = false)
    @NoView
    private Set<ImageBackupStorageRefVO> backupStorageRefs = new HashSet<ImageBackupStorageRefVO>();

    @Transient
    private String accountUuid;

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }


    public Set<ImageBackupStorageRefVO> getBackupStorageRefs() {
        return backupStorageRefs;
    }

    public void setBackupStorageRefs(Set<ImageBackupStorageRefVO> backupStorageRefs) {
        this.backupStorageRefs = backupStorageRefs;
    }
}
