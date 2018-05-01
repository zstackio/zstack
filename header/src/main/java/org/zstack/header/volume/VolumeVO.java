package org.zstack.header.volume;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EO;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table
@EO(EOClazz = VolumeEO.class)
@BaseResource
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
