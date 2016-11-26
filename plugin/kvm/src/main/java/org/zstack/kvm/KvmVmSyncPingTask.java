package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmTracer;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.logging.Log;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.rest.SyncHttpCallHandler;
import org.zstack.header.vm.*;
import org.zstack.kvm.KVMAgentCommands.ReportVmStateCmd;
import org.zstack.kvm.KVMAgentCommands.VmSyncCmd;
import org.zstack.kvm.KVMAgentCommands.VmSyncResponse;
import org.zstack.kvm.KVMConstant.KvmVmState;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KvmVmSyncPingTask extends VmTracer implements KVMPingAgentNoFailureExtensionPoint, KVMHostConnectExtensionPoint,
        HostConnectionReestablishExtensionPoint, HostAfterConnectedExtensionPoint, Component {
    private static final CLogger logger = Utils.getLogger(KvmVmSyncPingTask.class);
    
    @Autowired
    private RESTFacade restf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;

    private void syncVm(final HostInventory host, final Completion completion) {
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        VmSyncCmd cmd = new VmSyncCmd();
        msg.setCommand(cmd);
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
        msg.setNoStatusCheck(true);
        msg.setHostUuid(host.getUuid());
        msg.setPath(KVMConstant.KVM_VM_SYNC_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                VmSyncResponse ret = r.toResponse(VmSyncResponse.class);
                if (ret.isSuccess()) {
                    Map<String, VmInstanceState> states = new HashMap<String, VmInstanceState>(ret.getStates().size());
                    for (Map.Entry<String, String> e : ret.getStates().entrySet()) {
                        VmInstanceState state = KvmVmState.valueOf(e.getValue()).toVmInstanceState();
                        if (state == VmInstanceState.Running || state == VmInstanceState.Paused || state == VmInstanceState.Unknown) {
                            states.put(e.getKey(), state);
                        }
                    }

                    reportVmState(host.getUuid(), states);
                    completion.success();
                } else {
                    ErrorCode errorCode = errf.stringToOperationError(String.format("unable to do vm sync on host[uuid:%s, ip:%s] because %s", host.getUuid(), host.getManagementIp(), ret.getError()));
                    logger.warn(errorCode.toString());
                    completion.fail(errorCode);
                }
            }
        });
    }

    @Override
    public void connectionReestablished(HostInventory inv) throws HostException {
        syncVm(inv, new NopeCompletion());
    }

    @Override
    public HypervisorType getHypervisorTypeForReestablishExtensionPoint() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    @Override
    public boolean start() {
        restf.registerSyncHttpCallHandler(KVMConstant.KVM_REPORT_VM_STATE, ReportVmStateCmd.class, new SyncHttpCallHandler<ReportVmStateCmd>() {
            private void reportState(final ReportVmStateCmd cmd) {
                thdf.chainSubmit(new ChainTask() {
                    @Override
                    public String getSyncSignature() {
                        return String.format("report-state-of-vm-%s", cmd.vmUuid);
                    }

                    @Override
                    public void run(final SyncTaskChain chain) {
                        VmInstanceState state = KvmVmState.valueOf(cmd.vmState).toVmInstanceState();

                        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
                        q.select(VmInstanceVO_.state);
                        q.add(VmInstanceVO_.uuid, Op.EQ, cmd.vmUuid);
                        VmInstanceState stateInDb = q.findValue();
                        if (stateInDb == null) {
                            //TODO: handle anonymous vm
                            logger.warn(String.format("an anonymous VM[uuid:%s, state:%s] is detected on the host[uuid:%s]", cmd.hostUuid, state, cmd.hostUuid));
                            chain.next();
                            return;
                        }

                        VmStateChangedOnHostMsg msg = new VmStateChangedOnHostMsg();
                        msg.setVmStateAtTracingMoment(stateInDb);
                        msg.setVmInstanceUuid(cmd.vmUuid);
                        msg.setStateOnHost(state);
                        msg.setHostUuid(cmd.hostUuid);
                        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, cmd.vmUuid);
                        bus.send(msg, new CloudBusCallBack(chain) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    //TODO
                                    logger.warn(String.format("failed to report state[%s] of the vm[uuid:%s] on the host[uuid:%s]",
                                            cmd.vmState, cmd.vmUuid, cmd.hostUuid));
                                }

                                chain.next();
                            }
                        });
                    }

                    @Override
                    public String getName() {
                        return "report-vm-state";
                    }
                });
            }

            @Override
            public String handleSyncHttpCall(ReportVmStateCmd cmd) {
                reportState(cmd);
                return null;
            }
        });
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void afterHostConnected(final HostInventory host) {
        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.select(VmInstanceVO_.uuid);
        q.add(VmInstanceVO_.state, Op.EQ, VmInstanceState.Unknown);
        q.add(VmInstanceVO_.hostUuid, Op.EQ, host.getUuid());
        final List<String> vmUuids = q.listValue();
        if (!vmUuids.isEmpty()) {
            CheckVmStateOnHypervisorMsg msg = new CheckVmStateOnHypervisorMsg();
            msg.setVmInstanceUuids(vmUuids);
            msg.setHostUuid(host.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
            bus.send(msg, new CloudBusCallBack() {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        //TODO
                        logger.warn(String.format("unable to check states of vms[uuids:%s] on the host[uuid:%s], %s",
                                vmUuids, host.getUuid(), reply.getError()));
                        return;
                    }

                    CheckVmStateOnHypervisorReply r = reply.castReply();
                    Map<String, VmInstanceState> states = new HashMap<String, VmInstanceState>();
                    for (Map.Entry<String, String> e : r.getStates().entrySet()) {
                        states.put(e.getKey(), VmInstanceState.valueOf(e.getValue()));
                    }

                    reportVmState(host.getUuid(), states);
                }
            });
        }
    }

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                new Log(context.getInventory().getUuid()).log(KVMHostLabel.SYNC_VM_STATE);

                syncVm(context.getInventory(), new Completion(trigger) {
                    String __name__ = "sync-vm-state";

                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        };
    }

    @Override
    public void kvmPingAgentNoFailure(KVMHostInventory host, NoErrorCompletion completion) {
        if (!KVMGlobalConfig.VM_SYNC_ON_HOST_PING.value(Boolean.class)) {
            completion.done();
            return;
        }

        syncVm(host, new Completion(completion) {
            @Override
            public void success() {
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                //TODO
                logger.warn(String.format("failed to sync VM states on the KVM host[uuid:%s, name:%s, ip:%s], %s",
                        host.getUuid(), host.getName(), host.getManagementIp(), errorCode));
                completion.done();
            }
        });
    }
}
