package org.zstack.header.tag;

import org.zstack.header.vo.BaseResource;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 */
@Entity
@Table
@BaseResource
public class UserTagVO extends TagAO {
    public UserTagVO() {
        setType(TagType.User);
    }

    public UserTagVO(UserTagVO other) {
        super(other);
    }
}
