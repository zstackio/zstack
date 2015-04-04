package org.zstack.header.tag;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 */
@Entity
@Table
public class UserTagVO extends TagAO {
    public UserTagVO() {
        setType(TagType.User);
    }

    public UserTagVO(UserTagVO other) {
        super(other);
    }
}
