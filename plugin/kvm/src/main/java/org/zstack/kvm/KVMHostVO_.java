package org.zstack.kvm;

import org.zstack.header.host.HostVO_;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 */
@StaticMetamodel(KVMHostVO.class)
public class KVMHostVO_ extends HostVO_ {
    public static volatile SingularAttribute<KVMHostVO, String> username;
    //cannot get password by using this method,because password is encrypted
    public static volatile SingularAttribute<KVMHostVO, String> password;
    public static volatile SingularAttribute<KVMHostVO, Integer> port;
    public static volatile SingularAttribute<KVMHostVO, String> osDistribution;
    public static volatile SingularAttribute<KVMHostVO, String> osRelease;
    public static volatile SingularAttribute<KVMHostVO, String> osVersion;
}
