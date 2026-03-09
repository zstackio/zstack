package org.zstack.header.network.l3;

import jakarta.persistence.*;

@Entity
@Table
public class L3NetworkSequenceNumberVO {
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
