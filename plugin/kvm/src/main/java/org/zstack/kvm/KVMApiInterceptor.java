package org.zstack.kvm;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.APIAttachL2NetworkToClusterMsg;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2VlanNetworkVO;
import org.zstack.kvm.xmlhook.*;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.network.NetworkUtils;

import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

/**
 */
public class KVMApiInterceptor implements ApiMessageInterceptor, GlobalApiMessageInterceptor {
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddKVMHostMsg) {
            validate((APIAddKVMHostMsg) msg);
        } else if (msg instanceof APIAttachL2NetworkToClusterMsg) {
            validate((APIAttachL2NetworkToClusterMsg) msg);
        } else if (msg instanceof APICreateVmUserDefinedXmlHookScriptMsg) {
            validate((APICreateVmUserDefinedXmlHookScriptMsg) msg);
        } else if (msg instanceof APIUpdateVmUserDefinedXmlHookScriptMsg) {
            validate((APIUpdateVmUserDefinedXmlHookScriptMsg) msg);
        } else if (msg instanceof APIExpungeVmUserDefinedXmlHookScriptMsg) {
            validate((APIExpungeVmUserDefinedXmlHookScriptMsg) msg);
        }
        return msg;
    }

    private void validate(APIExpungeVmUserDefinedXmlHookScriptMsg msg) {
        checkSystemHookOrNot(msg);
        List<XmlHookVmInstanceRefVO> refVOs = Q.New(XmlHookVmInstanceRefVO.class)
                .eq(XmlHookVmInstanceRefVO_.xmlHookUuid, msg.getUuid()).list();
        if (refVOs != null && refVOs.size() > 0) {
            List<String> vmUuids = refVOs.stream()
                    .map(XmlHookVmInstanceRefVO::getVmInstanceUuid)
                    .collect(Collectors.toList());
            throw new ApiMessageInterceptionException(operr("the xml hook[%s] has been set to vm %s," +
                    " so unbind it before deleting it", msg.getUuid(), vmUuids));
        }
    }

    private void validate(APIUpdateVmUserDefinedXmlHookScriptMsg msg) {
        checkSystemHookOrNot(msg);
        String name = Q.New(XmlHookVO.class).select(XmlHookVO_.name)
                .eq(XmlHookVO_.name, msg.getName())
                .notEq(XmlHookVO_.uuid, msg.getUuid())
                .findValue();
        if (StringUtils.isNotEmpty(name)) {
            throw new ApiMessageInterceptionException(argerr("the xml hook name[%s] already exists", msg.getName()));
        }
    }

    private static void checkSystemHookOrNot(XmlHookMessage msg) {
        XmlHookVO vo = Q.New(XmlHookVO.class).eq(XmlHookVO_.uuid, msg.getXmlHookUuid()).find();
        if (XmlHookType.System.equals(vo.getType())) {
            throw new ApiMessageInterceptionException(operr("System-type xml hooks are not allowed to be modified"));
        }
    }

    private void validate(APICreateVmUserDefinedXmlHookScriptMsg msg) {
        String name = Q.New(XmlHookVO.class).select(XmlHookVO_.name)
                .eq(XmlHookVO_.name, msg.getName()).findValue();
        if (StringUtils.isNotEmpty(name)) {
            throw new ApiMessageInterceptionException(argerr("the xml hook name[%s] already exists", msg.getName()));
        }
    }


    private void validate(APIAddKVMHostMsg msg) {
        SimpleQuery<KVMHostVO> q = dbf.createQuery(KVMHostVO.class);
        q.add(KVMHostVO_.managementIp, Op.EQ, msg.getManagementIp());
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(argerr("there has been a kvm host having management ip[%s]", msg.getManagementIp()));
        }
    }

    // if attach l2 vlan network to kvm cluster, then check its interface length
    private void validate(APIAttachL2NetworkToClusterMsg msg) {
        ClusterVO cls = dbf.findByUuid(msg.getClusterUuid(), ClusterVO.class);
        if (cls != null && !cls.getHypervisorType().equals(KVMConstant.KVM_HYPERVISOR_TYPE)) {
            return;
        }

        L2VlanNetworkVO l2 = dbf.findByUuid(msg.getL2NetworkUuid(), L2VlanNetworkVO.class);
        if (l2 == null) {
            return;
        }

        if (NetworkUtils.generateVlanDeviceName(l2.getPhysicalInterface(), l2.getVlan()).length()
                > L2NetworkConstant.LINUX_IF_NAME_MAX_SIZE) {
            throw new ApiMessageInterceptionException(argerr("cannot create vlan-device on %s because it's too long"
                    , l2.getPhysicalInterface()));
        }
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return CollectionDSL.list(APIAttachL2NetworkToClusterMsg.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }
}
