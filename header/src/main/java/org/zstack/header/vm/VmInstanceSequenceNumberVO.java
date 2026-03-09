package org.zstack.header.vm;

import jakarta.persistence.*;

@Entity
@Table
public class VmInstanceSequenceNumberVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
