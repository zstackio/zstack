package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.volume.VolumeType;

import javax.persistence.Tuple;
import java.util.List;

/**
 * Created by MaJin on 2021/1/8.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmExtraInfoGetter {
    @Autowired
    private DatabaseFacade dbf;

    private String uuid;

    private VmExtraInfoGetter(String uuid) {
        this.uuid = uuid;
    }

    public static VmExtraInfoGetter New(String uuid) {
        return new VmExtraInfoGetter(uuid);
    }

    @Transactional(readOnly = true)
    public String getArchitecture() {
        List res = dbf.getEntityManager().createNativeQuery("select cluster.architecture, vm.hostUuid, vm.lastHostUuid" +
                " from VmInstanceVO vm" +
                " left join ClusterVO cluster on cluster.uuid = vm.clusterUuid" +
                " where vm.uuid = :uuid", Tuple.class)
                .setParameter("uuid", uuid)
                .getResultList();

        if (res.isEmpty()) {
            return null;
        }

        Tuple t = (Tuple) res.get(0);
        if (t.get(0) != null) {
            return t.get(0, String.class);
        }

        String hostUuid = t.get(1) != null ? t.get(1, String.class) : t.get(2, String.class);
        return SQL.New("select cluster.architecture from ClusterVO cluster, HostVO host" +
                " where host.uuid = :huuid" +
                " and cluster.uuid = host.clusterUuid", String.class)
                .param("huuid", hostUuid)
                .find();
    }

    public String getGuestOsType() {
        return  SQL.New("select i.guestOsType from VolumeVO v, ImageVO i" +
                " where v.vmInstanceUuid = :vmUuid" +
                " and v.rootImageUuid = i.uuid").
                param("vmUuid", uuid).find();
    }
}
