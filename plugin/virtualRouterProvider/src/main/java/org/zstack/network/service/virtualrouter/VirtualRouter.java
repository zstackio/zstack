package org.zstack.network.service.virtualrouter;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.appliancevm.*;
import org.zstack.appliancevm.ApplianceVmConstant.Params;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.workflow.*;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant.Param;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouter extends ApplianceVmBase {

    static {
        allowedOperations.addState(VmInstanceState.Running, APIReconnectVirtualRouterMsg.class.getName());
    }

    @Autowired
    private VirtualRouterManager vrMgr;
    @Autowired
    private RESTFacade restf;
    @Autowired
    private ErrorFacade errf;

    private VirtualRouterVmInventory vr;

    public VirtualRouter(ApplianceVmVO vo) {
        super(vo);
    }

    public VirtualRouter(VirtualRouterVmVO vo) {
        super(vo);
        vr = new VirtualRouterVmInventory(vo);
    }

    @Override
    protected List<Flow> getPostCreateFlows() {
        return vrMgr.getPostCreateFlows();
    }

    @Override
    protected List<Flow> getPostStartFlows() {
        return vrMgr.getPostStartFlows();
    }

    @Override
    protected List<Flow> getPostStopFlows() {
        return vrMgr.getPostStopFlows();
    }

    @Override
    protected List<Flow> getPostRebootFlows() {
        return vrMgr.getPostRebootFlows();
    }

    @Override
    protected List<Flow> getPostDestroyFlows() {
        return vrMgr.getPostDestroyFlows();
    }

    @Override
    protected List<Flow> getPostMigrateFlows() {
        return vrMgr.getPostMigrateFlows();
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIReconnectVirtualRouterMsg) {
            handle((APIReconnectVirtualRouterMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    @Override
    protected void handleLocalMessage(Message msg) {
        if (msg instanceof VirtualRouterAsyncHttpCallMsg) {
            handle((VirtualRouterAsyncHttpCallMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    private void handle(final VirtualRouterAsyncHttpCallMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("%s-commands", syncThreadName);
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final VirtualRouterAsyncHttpCallReply reply = new VirtualRouterAsyncHttpCallReply();
                if (msg.isCheckStatus() && getSelf().getStatus() != ApplianceVmStatus.Connected) {
                    reply.setError(errf.stringToOperationError(String.format("virtual router[uuid:%s] is in status of %s that cannot make http call to %s",
                            self.getUuid(), getSelf().getStatus(), msg.getPath())));
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                restf.asyncJsonPost(vrMgr.buildUrl(vr.getManagementNic().getIp(), msg.getPath()), msg.getCommand(), new JsonAsyncRESTCallback<LinkedHashMap>(msg, chain) {
                    @Override
                    public void fail(ErrorCode err) {
                        reply.setError(err);
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void success(LinkedHashMap ret) {
                        reply.setResponse(ret);
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public Class<LinkedHashMap> getReturnClass() {
                        return LinkedHashMap.class;
                    }
                }, TimeUnit.SECONDS, msg.getCommandTimeout());
            }

            @Override
            protected int getSyncLevel() {
                return vrMgr.getParallelismDegree(self.getUuid());
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(final APIReconnectVirtualRouterMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                reconnect(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("reconnect-virtual-router-%s", self.getUuid());
            }
        });
    }

    private void reconnect(APIReconnectVirtualRouterMsg msg, final NoErrorCompletion completion) {
        final APIReconnectVirtualRouterEvent evt = new APIReconnectVirtualRouterEvent(msg.getId());

        refreshVO();
        ErrorCode allowed = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
        if (allowed != null) {
            evt.setErrorCode(allowed);
            bus.publish(evt);
            completion.done();
            return;
        }

        FlowChain chain = vrMgr.getReconnectFlowChain();
        chain.setName(String.format("reconnect-virtual-router-%s", self.getUuid()));
        chain.getData().put(VirtualRouterConstant.Param.VR.toString(), vr);
        chain.getData().put(Param.IS_RECONNECT.toString(), Boolean.TRUE.toString());
        chain.getData().put(Params.isReconnect.toString(), Boolean.TRUE.toString());
        chain.getData().put(Params.managementNicIp.toString(), vr.getManagementNic().getIp());
        chain.getData().put(Params.applianceVmUuid.toString(), self.getUuid());

        SimpleQuery<ApplianceVmFirewallRuleVO> q = dbf.createQuery(ApplianceVmFirewallRuleVO.class);
        q.add(ApplianceVmFirewallRuleVO_.applianceVmUuid, Op.EQ, getSelf().getUuid());
        List<ApplianceVmFirewallRuleVO> vos = q.list();
        List<ApplianceVmFirewallRuleInventory> rules = ApplianceVmFirewallRuleInventory.valueOf(vos);
        chain.getData().put(ApplianceVmConstant.Params.applianceVmFirewallRules.toString(), rules);

        chain.insert(new Flow() {
            String __name__ = "change-appliancevm-status-to-connecting";
            ApplianceVmStatus originStatus = getSelf().getStatus();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                getSelf().setStatus(ApplianceVmStatus.Connecting);
                self = dbf.updateAndRefresh(self);
                trigger.next();
            }

            @Override
            public void rollback(FlowTrigger trigger, Map data) {
                self = dbf.reload(self);
                getSelf().setStatus(originStatus);
                self = dbf.updateAndRefresh(self);
                trigger.rollback();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "change-appliancevm-status-to-connected";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                getSelf().setStatus(ApplianceVmStatus.Connected);
                self = dbf.updateAndRefresh(self);
                trigger.next();
            }
        }).done(new FlowDoneHandler(msg, completion) {
            @Override
            public void handle(Map data) {
                self = dbf.reload(self);
                evt.setInventory(getInventory());
                bus.publish(evt);
                completion.done();
            }
        }).error(new FlowErrorHandler(msg, completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setErrorCode(errCode);
                bus.publish(evt);
                completion.done();
            }
        }).start();
    }
}
