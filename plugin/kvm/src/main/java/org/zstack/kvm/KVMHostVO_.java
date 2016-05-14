package org.zstack.kvm;

import org.zstack.header.host.HostVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 */
@StaticMetamodel(KVMHostVO.class)
public class KVMHostVO_ extends HostVO_ {
    public static volatile SingularAttribute<KVMHostVO, String> username;
    public static volatile SingularAttribute<KVMHostVO, String> password;
    public static volatile SingularAttribute<KVMHostVO, Integer> port;
}
