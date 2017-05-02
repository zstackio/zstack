package org.zstack.storage.primary.nfs; /**
 * Created by xing5 on 2016/8/19.
 */

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.storage.primary.APIUpdatePrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.vm.VmInstanceState;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class NfsApiParamChecker {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    public void checkUrl(String zoneUuid, String url) {
        SimpleQuery<PrimaryStorageVO> q = dbf.createQuery(PrimaryStorageVO.class);
        q.add(PrimaryStorageVO_.type, Op.EQ, NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE);
        q.add(PrimaryStorageVO_.url, Op.EQ, url);
        q.add(PrimaryStorageVO_.zoneUuid, Op.EQ, zoneUuid);
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(argerr("there has been a nfs primary storage having url as %s in zone[uuid:%s]", url, zoneUuid));
        }
    }

    public void checkRunningVmForUpdateUrl(String psuuid) {
        String sql = "select vm.name, vm.uuid from VmInstanceVO vm, VolumeVO vol where vm.uuid = vol.vmInstanceUuid and" +
                " vol.primaryStorageUuid = :psUuid and vm.state = :vmState";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("psUuid", psuuid);
        q.setParameter("vmState", VmInstanceState.Running);
        List<Tuple> ts = q.getResultList();

        if (!ts.isEmpty()) {
            List<String> vms = ts.stream().map(v -> String.format("VM[name:%s, uuid:%s]", v.get(0, String.class), v.get(1, String.class))).collect(Collectors.toList());
            throw new ApiMessageInterceptionException(operr("there are %s running VMs on the NFS primary storage, please" +
                    " stop them and try again:\n%s\n", vms.size(), StringUtils.join(vms, "\n")));
        }
    }
}
