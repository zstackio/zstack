package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusSteppingCallback;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.Q;
import org.zstack.core.plugin.FeaturePluginAgentExtensionPoint;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.host.ReconnectHostMsg;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Deploys the kvmagent parts of feature plugins with the kvm playbook: zstack-ctl keeps them and an index in the
 * kvm ansible module, the playbook runs on a host whose copy of the index differs.
 */
public class KvmFeaturePluginAgentDeployer implements FeaturePluginAgentExtensionPoint, KVMHostAddSshFileMd5CheckerExtensionPoint {
    private static final CLogger logger = Utils.getLogger(KvmFeaturePluginAgentDeployer.class);
    // where the kvm playbook keeps the index of the feature plugins it deployed
    private static final String HOST_INDEX = "/var/lib/zstack/kvm/package/feature-plugins/index";

    @Autowired
    private AnsibleFacade asf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ResourceDestinationMaker destMaker;

    private String index() {
        return PathUtil.join(PathUtil.getZStackHomeFolder(), "ansible", "files", "kvm", "feature-plugins", "index");
    }

    @Override
    public SshFileMd5Checker getSshFileMd5Checker(KVMHostVO host) {
        if (!new File(index()).exists()) {
            return null;
        }

        SshFileMd5Checker checker = new SshFileMd5Checker();
        checker.setUsername(host.getUsername());
        checker.setPassword(host.getPassword());
        checker.setSshPort(host.getPort());
        checker.setTargetIp(host.getManagementIp());
        checker.addSrcDestPair(index(), HOST_INDEX);
        return checker;
    }

    @Override
    public void agentPartsChanged() {
        asf.deployModule(KVMConstant.ANSIBLE_MODULE_PATH, KVMConstant.ANSIBLE_PLAYBOOK_NAME);
        List<String> hostUuids = Q.New(HostVO.class).select(HostVO_.uuid)
                .eq(HostVO_.hypervisorType, KVMConstant.KVM_HYPERVISOR_TYPE)
                .eq(HostVO_.status, HostStatus.Connected)
                .listValues();
        List<ReconnectHostMsg> msgs = hostUuids.stream().filter(destMaker::isManagedByUs).map(uuid -> {
            ReconnectHostMsg msg = new ReconnectHostMsg();
            msg.setHostUuid(uuid);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, uuid);
            return msg;
        }).collect(Collectors.toList());
        if (msgs.isEmpty()) {
            return;
        }

        logger.info(String.format("reconnect %s hosts to deploy the changed kvmagent parts of feature plugins", msgs.size()));
        bus.send(msgs, HostGlobalConfig.HOST_LOAD_PARALLELISM_DEGREE.value(Integer.class), new CloudBusSteppingCallback(null) {
            @Override
            public void run(NeedReplyMessage msg, MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to reconnect host[uuid:%s] for feature plugins: %s",
                            ((ReconnectHostMsg) msg).getHostUuid(), reply.getError()));
                }
            }
        });
    }
}
