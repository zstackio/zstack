package org.zstack.header.core.captcha;

import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.sql.Timestamp;

/**
 * Created by kayo on 2018/8/6.
 */
@Entity
@Table
@BaseResource
public class LoginAttemptsVO extends ResourceVO {
    @Column
    private String targetResourceIdentity;

    @Column
    private Integer attempts;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getTargetResourceIdentity() {
        return targetResourceIdentity;
    }

    public void setTargetResourceIdentity(String targetResourceIdentity) {
        this.targetResourceIdentity = targetResourceIdentity;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
