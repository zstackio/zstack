package org.zstack.header.configuration;

import org.zstack.header.vo.EO;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table
@EO(EOClazz = InstanceOfferingEO.class)
public class InstanceOfferingVO extends InstanceOfferingAO {
    public InstanceOfferingVO() {
    }

    public InstanceOfferingVO(InstanceOfferingVO other) {
        super(other);
    }
}

