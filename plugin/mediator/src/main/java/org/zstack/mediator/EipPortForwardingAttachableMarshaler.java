package org.zstack.mediator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.ReplyMessagePreSendingExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.message.Message;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.eip.APIGetEipAttachableVmNicsReply;
import org.zstack.network.service.portforwarding.APIGetPortForwardingAttachableVmNicsReply;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class EipPortForwardingAttachableMarshaler implements ReplyMessagePreSendingExtensionPoint {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public List<Class> getReplyMessageClassForPreSendingExtensionPoint() {
        List<Class> clz = new ArrayList<Class>();
        clz.add(APIGetEipAttachableVmNicsReply.class);
        clz.add(APIGetPortForwardingAttachableVmNicsReply.class);
        return clz;
    }

    @Override
    public void marshalReplyMessageBeforeSending(Message msg) {
        if (msg instanceof APIGetEipAttachableVmNicsReply) {
            marshal((APIGetEipAttachableVmNicsReply) msg);
        } else if (msg instanceof APIGetPortForwardingAttachableVmNicsReply) {
            marshal((APIGetPortForwardingAttachableVmNicsReply) msg);
        }
    }

    @Transactional(readOnly = true)
    private void marshal(APIGetPortForwardingAttachableVmNicsReply msg) {
        if (msg.getInventories().isEmpty()) {
            return;
        }

        List<String> nicUuids = CollectionUtils.transformToList(msg.getInventories(), new Function<String, VmNicInventory>() {
            @Override
            public String call(VmNicInventory arg) {
                return arg.getUuid();
            }
        });

        String sql = "select nic.uuid from VmNicVO nic, EipVO eip where nic.uuid in (:nicUuids) and eip.vmNicUuid = nic.uuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("nicUuids", nicUuids);
        List<String> uuids = q.getResultList();
        if (!uuids.isEmpty()) {
            List<VmNicInventory> ret = new ArrayList<VmNicInventory>(msg.getInventories().size());
            for (VmNicInventory nic : msg.getInventories()) {
                if (uuids.contains(nic.getUuid())) {
                    continue;
                }
                ret.add(nic);
            }
            msg.setInventories(ret);
        }
    }

    @Transactional(readOnly = true)
    private void marshal(APIGetEipAttachableVmNicsReply msg) {
        if (msg.getInventories().isEmpty()) {
            return;
        }

        List<String> nicUuids = CollectionUtils.transformToList(msg.getInventories(), new Function<String, VmNicInventory>() {
            @Override
            public String call(VmNicInventory arg) {
                return arg.getUuid();
            }
        });

        String sql = "select nic.uuid from VmNicVO nic, PortForwardingRuleVO pf where nic.uuid in (:nicUuids) and pf.vmNicUuid = nic.uuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("nicUuids", nicUuids);
        List<String> uuids = q.getResultList();
        if (!uuids.isEmpty()) {
            List<VmNicInventory> ret = new ArrayList<VmNicInventory>(msg.getInventories().size());
            for (VmNicInventory nic : msg.getInventories()) {
                if (uuids.contains(nic.getUuid())) {
                    continue;
                }
                ret.add(nic);
            }
            msg.setInventories(ret);
        }
    }
}
