package org.zstack.header.volume;

import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.image.ImageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.EntityGraph;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table
@EO(EOClazz = VolumeEO.class)
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = PrimaryStorageVO.class, myField = "primaryStorageUuid", targetField = "uuid"),
        },

        friends = {
                @EntityGraph.Neighbour(type = VmInstanceVO.class, myField = "vmInstanceUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = ImageVO.class, myField = "rootImageUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = DiskOfferingVO.class, myField = "diskOfferingUuid", targetField = "uuid"),
        }
)
public class VolumeVO extends VolumeAO implements OwnedByAccount {
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

}
