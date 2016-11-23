package org.zstack.appliancevm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.appliancevm.ApplianceVmConstant.BootstrapParams;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginExtension;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.job.JobQueueFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class ApplianceVmFacadeImpl extends AbstractService implements ApplianceVmFacade, Component {
    private static final CLogger logger = Utils.getLogger(ApplianceVmFacadeImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private JobQueueFacade jobf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private GlobalConfigFacade gcf;
    @Autowired
    private AnsibleFacade asf;

    private List<String> createApplianceVmWorkFlow;
    private FlowChainBuilder createApplianceVmWorkFlowBuilder;
    private Map<String, ApplianceVmBootstrapFlowFactory> bootstrapInfoFlowFactories = new HashMap<String, ApplianceVmBootstrapFlowFactory>();

    private String OWNER = String.format("ApplianceVm.%s", Platform.getManagementServerId());

    public void createApplianceVm(ApplianceVmSpec spec, final ReturnValueCompletion<ApplianceVmInventory> completion) {
        CreateApplianceVmJob job = new CreateApplianceVmJob();
        job.setSpec(spec);
        if (!spec.isSyncCreate()) {
            job.run(new ReturnValueCompletion<Object>(completion) {
                @Override
                public void success(Object returnValue) {
                    completion.success((ApplianceVmInventory) returnValue);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    completion.fail(errorCode);
                }
            });
        } else {
            jobf.execute(spec.getName(), OWNER, job, completion, ApplianceVmInventory.class);
        }
    }

    @Override
    public void startApplianceVm(final String vmUuid, final ReturnValueCompletion<ApplianceVmInventory> completion) {
        StartVmInstanceMsg msg = new StartVmInstanceMsg();
        msg.setVmInstanceUuid(vmUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    ApplianceVmVO vo = dbf.findByUuid(vmUuid, ApplianceVmVO.class);
                    completion.success(ApplianceVmInventory.valueOf(vo));
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void stopApplianceVm(final String vmUuid, final ReturnValueCompletion<ApplianceVmInventory> completion) {
        StopVmInstanceMsg msg = new StopVmInstanceMsg();
        msg.setVmInstanceUuid(vmUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    ApplianceVmVO vo = dbf.findByUuid(vmUuid, ApplianceVmVO.class);
                    completion.success(ApplianceVmInventory.valueOf(vo));
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void rebootApplianceVm(final String vmUuid, final ReturnValueCompletion<ApplianceVmInventory> completion) {
        RebootVmInstanceMsg msg = new RebootVmInstanceMsg();
        msg.setVmInstanceUuid(vmUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    ApplianceVmVO vo = dbf.findByUuid(vmUuid, ApplianceVmVO.class);
                    completion.success(ApplianceVmInventory.valueOf(vo));
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void destroyApplianceVm(final String vmUuid, final ReturnValueCompletion<ApplianceVmInventory> completion) {
        final ApplianceVmVO vo = dbf.findByUuid(vmUuid, ApplianceVmVO.class);
        DestroyVmInstanceMsg msg = new DestroyVmInstanceMsg();
        msg.setVmInstanceUuid(vmUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success(ApplianceVmInventory.valueOf(vo));
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void destroyApplianceVm(String vmUuid) {
        destroyApplianceVm(vmUuid, new ReturnValueCompletion<ApplianceVmInventory>() {
            @Override
            public void success(ApplianceVmInventory returnValue) {
            }

            @Override
            public void fail(ErrorCode errorCode) {
            }
        });
    }

    public List<String> getCreateApplianceVmWorkFlow() {
        return createApplianceVmWorkFlow;
    }

    public void setCreateApplianceVmWorkFlow(List<String> createApplianceVmWorkFlow) {
        this.createApplianceVmWorkFlow = createApplianceVmWorkFlow;
    }

    private void populateExtensions() {
        List<PluginExtension> exts = pluginRgty.getExtensionByInterfaceName(ApplianceVmBootstrapFlowFactory.class.getName());
        for (PluginExtension ext : exts) {
            ApplianceVmBootstrapFlowFactory extp = (ApplianceVmBootstrapFlowFactory) ext.getInstance();
            ApplianceVmBootstrapFlowFactory old = bootstrapInfoFlowFactories.get(extp.getHypervisorTypeForApplianceVmBootstrapFlow().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("two extensions[%s, %s] declare ApplianceVmBootstrapFlowFactory for hypervisor type[%s]", old.getClass().getName(), extp.getClass().getName(), extp.getHypervisorTypeForApplianceVmBootstrapFlow()));
            }

            bootstrapInfoFlowFactories.put(extp.getHypervisorTypeForApplianceVmBootstrapFlow().toString(), extp);
        }
    }

    private void deployAnsible() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        asf.deployModule(ApplianceVmConstant.ANSIBLE_MODULE_PATH, ApplianceVmConstant.ANSIBLE_PLAYBOOK_NAME);
    }

    @Override
    public boolean start() {
        createApplianceVmWorkFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(getCreateApplianceVmWorkFlow()).construct();
        populateExtensions();
        deployAnsible();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    public FlowChainBuilder getCreateApplianceVmWorkFlowBuilder() {
        return createApplianceVmWorkFlowBuilder;
    }

    private List<VmNicInventory> reduceNic(List<VmNicInventory> nics, VmNicInventory nic) {
        List<VmNicInventory> ret = new ArrayList<VmNicInventory>();
        for (VmNicInventory n : nics) {
            if (n.getUuid().equals(nic.getUuid())) {
                continue;
            }

            ret.add(n);
        }
        return ret;
    }

    @Override
    public Map<String, Object> prepareBootstrapInformation(VmInstanceSpec spec) {
        VmNicInventory mgmtNic = null;
        String defaultL3Uuid;
        int sshPort;
        if (spec.getCurrentVmOperation() == VmInstanceConstant.VmOperation.NewCreate) {
            ApplianceVmSpec aspec = spec.getExtensionData(ApplianceVmConstant.Params.applianceVmSpec.toString(), ApplianceVmSpec.class);
            for (VmNicInventory nic : spec.getDestNics()) {
                if (nic.getL3NetworkUuid().equals(aspec.getManagementNic().getL3NetworkUuid())) {
                    mgmtNic = nic;
                    break;
                }
            }

            DebugUtils.Assert(mgmtNic!=null, String.format("cannot find management nic for appliance vm[uuid:%s]", aspec.getUuid()));
            defaultL3Uuid = aspec.getDefaultRouteL3Network() != null ? aspec.getDefaultRouteL3Network().getUuid() : mgmtNic.getL3NetworkUuid();
            sshPort = aspec.getSshPort();
        } else {
            ApplianceVmVO avo = dbf.findByUuid(spec.getVmInventory().getUuid(), ApplianceVmVO.class);
            ApplianceVmInventory ainv = ApplianceVmInventory.valueOf(avo);
            mgmtNic = ainv.getManagementNic();
            defaultL3Uuid = ainv.getDefaultRouteL3NetworkUuid();
            //TODO: make it configurable
            sshPort = 22;
        }

        Map<String, Object> ret = new HashMap<String, Object>();
        ApplianceVmNicTO mto = new ApplianceVmNicTO(mgmtNic);
        mto.setDeviceName(String.format("eth0"));
        if (mgmtNic.getL3NetworkUuid().equals(defaultL3Uuid)) {
            mto.setDefaultRoute(true);
        }
        ret.put(ApplianceVmConstant.BootstrapParams.managementNic.toString(), mto);

        List<ApplianceVmNicTO> extraTos = new ArrayList<ApplianceVmNicTO>();
        ret.put(ApplianceVmConstant.BootstrapParams.additionalNics.toString(), extraTos);

        List<VmNicInventory> additionalNics = reduceNic(spec.getDestNics(), mgmtNic);
        // if management nic is not default route nic, choose default route nic as eth1
        int deviceId = 1;
        if (!mto.isDefaultRoute()) {
            VmNicInventory defaultRouteNic = null;
            for (VmNicInventory nic : additionalNics) {
                if (nic.getL3NetworkUuid().equals(defaultL3Uuid)) {
                    defaultRouteNic = nic;
                    break;
                }
            }

            ApplianceVmNicTO t = new ApplianceVmNicTO(defaultRouteNic);
            t.setDeviceName(String.format("eth%s", deviceId));
            t.setDefaultRoute(true);
            deviceId ++;
            extraTos.add(t);
            additionalNics = reduceNic(additionalNics, defaultRouteNic);
        }

        for (VmNicInventory nic : additionalNics) {
            ApplianceVmNicTO nto = new ApplianceVmNicTO(nic);
            nto.setDeviceName(String.format("eth%s", deviceId));
            extraTos.add(nto);
            deviceId ++;
        }

        String publicKey = asf.getPublicKey();
        ret.put(ApplianceVmConstant.BootstrapParams.publicKey.toString(), publicKey);
        ret.put(BootstrapParams.sshPort.toString(), sshPort);

        for (ApplianceVmPrepareBootstrapInfoExtensionPoint ext : pluginRgty.getExtensionList(ApplianceVmPrepareBootstrapInfoExtensionPoint.class)) {
            ext.applianceVmPrepareBootstrapInfo(spec, ret);
        }

        return ret;
    }


    @Override
    public Flow createBootstrapFlow(HypervisorType hvType) {
        ApplianceVmBootstrapFlowFactory factory = bootstrapInfoFlowFactories.get(hvType.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("unable to find ApplianceVmBootstrapFlowFactory for hypervisor type[%s]", hvType));
        }

        return factory.createApplianceVmBootstrapInfoFlow();
    }

    void openFirewallInBootstrap(String apvmUuid, final String l3uuid, List<ApplianceVmFirewallRuleInventory> rules, final Completion completion) {
        openFirewall(apvmUuid, l3uuid, rules, false, completion);
    }

    private void openFirewall(final String apvmUuid, final String l3uuid, final List<ApplianceVmFirewallRuleInventory> rules, boolean needVmSync, final Completion completion) {
        final ApplianceVmVO apvm = dbf.findByUuid(apvmUuid, ApplianceVmVO.class);

        VmNicInventory targetNic = CollectionUtils.find(apvm.getVmNics(), new Function<VmNicInventory, VmNicVO>() {
            @Override
            public VmNicInventory call(VmNicVO arg) {
                if (arg.getL3NetworkUuid().equals(l3uuid)) {
                    return VmNicInventory.valueOf(arg);
                }
                return null;
            }

        });
        DebugUtils.Assert(targetNic!=null, String.format("appliance vm[uuid:%s, name:%s] is not on l3network[uuid:%s]", apvm.getUuid(), apvm.getName(), l3uuid));

        List<String> ids = CollectionUtils.transformToList(rules, new Function<String, ApplianceVmFirewallRuleInventory>() {
            @Override
            public String call(ApplianceVmFirewallRuleInventory arg) {
                arg.setL3NetworkUuid(l3uuid);
                arg.setApplianceVmUuid(apvmUuid);
                return arg.makeIdentity();
            }
        });

        SimpleQuery<ApplianceVmFirewallRuleVO> q = dbf.createQuery(ApplianceVmFirewallRuleVO.class);
        q.select(ApplianceVmFirewallRuleVO_.identity);
        q.add(ApplianceVmFirewallRuleVO_.identity, Op.IN, ids);
        final List<String> existingIds = q.listValue();

        List<ApplianceVmFirewallRuleVO> vos = CollectionUtils.transformToList(rules, new Function<ApplianceVmFirewallRuleVO, ApplianceVmFirewallRuleInventory>() {
            @Override
            public ApplianceVmFirewallRuleVO call(ApplianceVmFirewallRuleInventory r) {
                if (!existingIds.contains(r.makeIdentity())) {
                    ApplianceVmFirewallRuleVO vo = new ApplianceVmFirewallRuleVO();
                    vo.setSourceIp(r.getSourceIp());
                    vo.setDestIp(r.getDestIp());
                    vo.setAllowCidr(r.getAllowCidr());
                    vo.setProtocol(ApplianceVmFirewallProtocol.valueOf(r.getProtocol()));
                    vo.setEndPort(r.getEndPort());
                    vo.setStartPort(r.getStartPort());
                    vo.setL3NetworkUuid(l3uuid);
                    vo.setApplianceVmUuid(apvm.getUuid());
                    vo.makeIdentity();
                    return vo;
                }
                return null;
            }
        });

        dbf.persistCollection(vos);

        ApplianceVmRefreshFirewallMsg msg = new ApplianceVmRefreshFirewallMsg();
        msg.setVmInstanceUuid(apvm.getUuid());
        msg.setInSyncThread(needVmSync);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, msg.getVmInstanceUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void openFirewall(String apvmUuid, final String l3uuid, List<ApplianceVmFirewallRuleInventory> rules, final Completion completion) {
        openFirewall(apvmUuid, l3uuid, rules, true, completion);
    }

    private void removeFirewall(final String applianceVmUuid, final String l3uuid, List<ApplianceVmFirewallRuleInventory> rules, boolean needVmSync, final Completion completion) {
        final List<String> ids = CollectionUtils.transformToList(rules, new Function<String, ApplianceVmFirewallRuleInventory>() {
            @Override
            public String call(ApplianceVmFirewallRuleInventory r) {
                r.setL3NetworkUuid(l3uuid);
                r.setApplianceVmUuid(applianceVmUuid);
                return r.makeIdentity();
            }
        });

        new Runnable() {
            @Override
            @Transactional
            public void run() {
                String sql = "delete from ApplianceVmFirewallRuleVO r where r.identity in (:ids)";
                Query q = dbf.getEntityManager().createQuery(sql);
                q.setParameter("ids", ids);
                q.executeUpdate();
            }
        }.run();

        ApplianceVmRefreshFirewallMsg msg = new ApplianceVmRefreshFirewallMsg();
        msg.setVmInstanceUuid(applianceVmUuid);
        msg.setInSyncThread(needVmSync);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, msg.getVmInstanceUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void removeFirewall(String applianceVmUuid, String l3uuid, List<ApplianceVmFirewallRuleInventory> rules, final Completion completion) {
        removeFirewall(applianceVmUuid, l3uuid, rules, true, completion);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage)msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIListApplianceVmMsg) {
            handle((APIListApplianceVmMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIListApplianceVmMsg msg) {
        List<ApplianceVmVO> vos = dbf.listAll(ApplianceVmVO.class);
        List<ApplianceVmInventory> invs = ApplianceVmInventory.valueOf1(vos);
        APIListApplianceVmReply reply = new APIListApplianceVmReply();
        reply.setInventories(invs);
        bus.reply(msg, reply);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(ApplianceVmConstant.SERVICE_ID);
    }
}
