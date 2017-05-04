package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.network.l2.L2Errors;
import org.zstack.header.network.l2.L2Network;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.network.l2.L2NetworkManager;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO_;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.List;

/**
 * Created by weiwang on 10/03/2017.
 */
public abstract class AbstractVniAllocatorStrategy implements VniAllocatorStrategy {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected L2NetworkManager l2NwMgr;
    @Autowired
    protected ErrorFacade errf;

    protected Integer allocateRequiredVni(VniAllocateMessage msg) {
        List<VniRangeVO> vnirs = Q.New(VniRangeVO.class).eq(VniRangeVO_.l2NetworkUuid, msg.getL2NetworkUuid()).list();

        final int rvni = msg.getRequiredVni();

        VniRangeVO vnir = CollectionUtils.find(vnirs, new Function<VniRangeVO, VniRangeVO>() {
            @Override
            public VniRangeVO call(VniRangeVO arg) {
                int s = arg.getStartVni();
                int e = arg.getEndVni();
                return s <= rvni && rvni <= e ? arg : null;
            }
        });

        String duplicate = Q.New(VxlanNetworkVO.class).select(VxlanNetworkVO_.uuid).eq(VxlanNetworkVO_.vni, msg.getRequiredVni()).eq(VxlanNetworkVO_.poolUuid, msg.getL2NetworkUuid()).findValue();

        if (vnir == null) {
            throw new OperationFailureException(errf.instantiateErrorCode(L2Errors.ALLOCATE_VNI_ERROR,
                    String.format("cannot allocate vni[%s] in l2Network[uuid:%s], out of vni range", msg.getRequiredVni(), msg.getL2NetworkUuid())
            ));
        } else if (duplicate != null) {
            throw new OperationFailureException(errf.instantiateErrorCode(L2Errors.ALLOCATE_VNI_ERROR,
                    String.format("cannot allocate vni[%s] in l2Network[uuid:%s], duplicate with l2Network[uuid:%s]", msg.getRequiredVni(), msg.getL2NetworkUuid(), duplicate)
            ));
        }

        return rvni;
    }
}
