package org.zstack.header.configuration;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EO;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table
@EO(EOClazz = DiskOfferingEO.class)
@BaseResource
@AutoDeleteTag
public class DiskOfferingVO extends DiskOfferingAO implements OwnedByAccount {
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
