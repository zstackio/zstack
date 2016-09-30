package org.zstack.header.identity;

import javax.persistence.*;
import java.util.Date;

@Entity
public class CurrentDateVO {
    @Id
    @Column(name = "DATE_VALUE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date date;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
