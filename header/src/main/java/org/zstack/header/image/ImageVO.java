package org.zstack.header.image;

import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.NoView;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@EO(EOClazz = ImageEO.class)
@BaseResource
public class ImageVO extends ImageAO {
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "imageUuid", insertable = false, updatable = false)
    @NoView
    private Set<ImageBackupStorageRefVO> backupStorageRefs = new HashSet<ImageBackupStorageRefVO>();

    public Set<ImageBackupStorageRefVO> getBackupStorageRefs() {
        return backupStorageRefs;
    }

    public void setBackupStorageRefs(Set<ImageBackupStorageRefVO> backupStorageRefs) {
        this.backupStorageRefs = backupStorageRefs;
    }
}
