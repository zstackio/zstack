package org.zstack.kvm;

import org.hibernate.search.annotations.Indexed;
import org.zstack.core.convert.PasswordConverter;
import org.zstack.header.host.HostEO;
import org.zstack.header.host.HostVO;
import org.zstack.header.vo.EO;

import javax.persistence.*;

@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
@EO(EOClazz = HostEO.class, needView = false)
@Indexed
public class KVMHostVO extends HostVO {
    @Column
    private String username;
    
    @Column
    @Convert(converter = PasswordConverter.class)
    private String password;

    @Column
    private Integer port;

    public KVMHostVO() {
    }

    public KVMHostVO(HostVO vo) {
        super(vo);
    }
    
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

}

