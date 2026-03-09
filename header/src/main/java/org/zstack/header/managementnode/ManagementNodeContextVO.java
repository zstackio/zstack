package org.zstack.header.managementnode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table
public class ManagementNodeContextVO {
    @Id
    @Column
    private final long id;

    @Column
    private byte[] inventory;

    public ManagementNodeContextVO() {
        id = 1;
    }

    public ManagementNodeContextVO(byte[] inventory) {
        super();
        id = 1;
        this.inventory = inventory;
    }


    public byte[] getInventory() {
        return inventory;
    }


    public void setInventory(byte[] inventory) {
        this.inventory = inventory;
    }

    public long getId() {
        return id;
    }
}
