package org.zstack.console;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.Component;
import org.zstack.header.console.ConsoleBackend;
import org.zstack.header.console.ConsoleConstants;
import org.zstack.header.console.ConsoleInventory;
import org.zstack.header.console.ConsoleProxy;
import org.zstack.header.console.ConsoleProxyInventory;
import org.zstack.header.console.ConsoleProxyStatus;
import org.zstack.header.console.ConsoleProxyVO;
import org.zstack.header.console.ConsoleProxyVO_;
import org.zstack.header.core.AsyncLatch;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 7:32 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractConsoleProxyBackend implements ConsoleBackend, Component, ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(AbstractConsoleProxyBackend.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected RESTFacade restf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected AnsibleFacade asf;
    @Autowired
    protected ErrorFacade errf;

    protected static final String ANSIBLE_PLAYBOOK_NAME = "consoleproxy.py";

    protected abstract ConsoleProxy getConsoleProxy(VmInstanceInventory vm, ConsoleProxyVO vo);

    protected abstract ConsoleProxy getConsoleProxy(SessionInventory session, VmInstanceInventory vm);

    protected abstract ConsoleProxy getConsoleProxy(ConsoleProxyInventory proxy);

    protected abstract void connectAgent();

    protected abstract boolean isAgentConnected();

    private void establishNewProxy(ConsoleProxy proxy, SessionInventory session, final VmInstanceInventory vm, final ReturnValueCompletion<ConsoleInventory> complete) {
        proxy.establishProxy(session, vm, new ReturnValueCompletion<ConsoleProxyInventory>(complete) {
            @Override
            public void success(ConsoleProxyInventory ret) {
                ConsoleProxyVO vo = new ConsoleProxyVO();
                vo.setAgentIp(ret.getAgentIp());
                vo.setProxyIdentity(ret.getProxyIdentity());
                vo.setScheme(ret.getScheme());
                vo.setProxyHostname(ret.getProxyHostname());
                vo.setProxyPort(ret.getProxyPort());
                vo.setTargetSchema(ret.getTargetSchema());
                vo.setTargetHostname(ret.getTargetHostname());
                vo.setTargetPort(ret.getTargetPort());
                vo.setToken(ret.getToken());
                vo.setVmInstanceUuid(vm.getUuid());
                vo.setUuid(Platform.getUuid());
                vo.setAgentType(ret.getAgentType());
                vo.setStatus(ConsoleProxyStatus.Active);
                vo.setVersion(ret.getVersion());
                vo.setExpiredDate(ret.getExpiredDate());
                vo = dbf.persistAndRefresh(vo);

                complete.success(ConsoleInventory.valueOf(vo));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                complete.fail(errorCode);
            }
        });
    }

    @Override
    public void grantConsoleAccess(final SessionInventory session, final VmInstanceInventory vm, final ReturnValueCompletion<ConsoleInventory> complete) {
        if (!isAgentConnected()) {
            complete.fail(operr("the console agent is not connected; it's mostly like the management node just starts, " +
                    "please wait for the console agent connected, or you can reconnect it manually if disconnected for a long time."
            ));
            return;
        }

        SimpleQuery<ConsoleProxyVO> q = dbf.createQuery(ConsoleProxyVO.class);
        q.add(ConsoleProxyVO_.vmInstanceUuid, SimpleQuery.Op.EQ, vm.getUuid());
        q.add(ConsoleProxyVO_.status, SimpleQuery.Op.EQ, ConsoleProxyStatus.Active);
        final ConsoleProxyVO vo = q.find();

        if (vo == null) {
            // new console proxy
            ConsoleProxy proxy = getConsoleProxy(session, vm);
            establishNewProxy(proxy, session, vm, complete);
            return;
        }


        String hostIp = getHostIp(vm);
        if (hostIp == null) {
            throw new OperationFailureException(operr("cannot find host IP of the vm[uuid:%s], is the vm running???", vm.getUuid()));
        }

        if (vo.getTargetHostname().equals(hostIp)) {
            // vm is on the same host
            final ConsoleProxy proxy = getConsoleProxy(vm, vo);
            dbf.remove(vo);
            establishNewProxy(proxy, session, vm, complete);
        } else {
            // vm is on another host
            FlowChain chain = FlowChainBuilder.newShareFlowChain();
            chain.setName(String.format("recreate-console-for-vm-%s", vm.getUuid()));
            chain.then(new ShareFlow() {
                ConsoleInventory ret;

                @Override
                public void setup() {
                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-old-console";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            deleteConsoleSession(vm, new Completion(trigger) {
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
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "create-new-console";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            ConsoleProxy proxy = getConsoleProxy(session, vm);
                            establishNewProxy(proxy, session, vm, new ReturnValueCompletion<ConsoleInventory>(trigger) {
                                @Override
                                public void success(ConsoleInventory returnValue) {
                                    ret = returnValue;
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                    });

                    done(new FlowDoneHandler(complete) {
                        @Override
                        public void handle(Map data) {
                            complete.success(ret);
                        }
                    });

                    error(new FlowErrorHandler(complete) {
                        @Override
                        public void handle(ErrorCode errCode, Map data) {
                            complete.fail(errCode);
                        }
                    });
                }
            }).start();
        }
    }

    @Transactional(readOnly = true)
    protected String getHostIp(VmInstanceInventory vm) {
        String sql = "select h.managementIp from HostVO h, VmInstanceVO vm where h.uuid = vm.hostUuid and vm.uuid = :uuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("uuid", vm.getUuid());
        List<String> ret = q.getResultList();
        if (!ret.isEmpty()) {
            return ret.get(0);
        }

        // FIXME
        sql = "select g.managementIp from BareMetal2GatewayVO g, BareMetal2InstanceVO bm where g.uuid = bm.gatewayUuid and bm.uuid = :uuid";
        q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("uuid", vm.getUuid());
        ret = q.getResultList();
        return ret.isEmpty() ? null : ret.get(0);
    }

    @Override
    public void deleteConsoleSession(SessionInventory session, final NoErrorCompletion completion) {
        SimpleQuery<ConsoleProxyVO> q = dbf.createQuery(ConsoleProxyVO.class);
        q.add(ConsoleProxyVO_.token, Op.LIKE, session.getUuid() + "%");
        List<ConsoleProxyVO> vos = q.list();

        if (vos.isEmpty()) {
            completion.done();
            return;
        }

        final AsyncLatch latch = new AsyncLatch(vos.size(), new NoErrorCompletion(completion) {
            @Override
            public void done() {
                completion.done();
            }
        });

        for (final ConsoleProxyVO vo : vos) {
            final VmInstanceVO vm = dbf.findByUuid(vo.getVmInstanceUuid(), VmInstanceVO.class);
            if (vm == null) {
                latch.ack();
                continue;
            }

            VmInstanceInventory vminv = VmInstanceInventory.valueOf(vm);
            ConsoleProxy proxy = getConsoleProxy(vminv, vo);
            proxy.deleteProxy(vminv, new Completion(latch) {
                @Override
                public void success() {
                    dbf.remove(vo);
                    logger.debug(String.format("deleted a console proxy[vmUuid:%s, host IP: %s, host port: %s, proxy IP: %s, proxy port: %s",
                            vm.getUuid(), vo.getTargetHostname(), vo.getTargetPort(), vo.getProxyHostname(), vo.getProxyPort()));
                    latch.ack();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.warn(String.format("failed to delete a console proxy[vmUuid:%s, host IP: %s, host port: %s, proxy IP: %s, proxy port: %s], %s",
                            vm.getUuid(), vo.getTargetHostname(), vo.getTargetPort(), vo.getProxyHostname(), vo.getProxyPort(), errorCode.toString()));
                    latch.ack();
                }
            });
        }

    }

    @Override
    public void deleteConsoleSession(final VmInstanceInventory vm, final Completion completion) {
        SimpleQuery<ConsoleProxyVO> q = dbf.createQuery(ConsoleProxyVO.class);
        q.add(ConsoleProxyVO_.vmInstanceUuid, SimpleQuery.Op.EQ, vm.getUuid());
        q.add(ConsoleProxyVO_.status, SimpleQuery.Op.EQ, ConsoleProxyStatus.Active);
        final ConsoleProxyVO vo = q.find();
        if (vo != null) {
            // wss do not request console proxy because it connect to vm's host directly
            // so skip deleteProxy
            // TODO: use ConsoleUrl returned needConsoleProxy as vo attribute for checking instead of schema
            if (ConsoleConstants.WSS_SCHEMA.equals(vo.getScheme())) {
                dbf.remove(vo);
                completion.success();
                return;
            }

            ConsoleProxy proxy = getConsoleProxy(vm, vo);
            proxy.deleteProxy(vm, new Completion(completion) {
                @Override
                public void success() {
                    dbf.remove(vo);
                    logger.debug(String.format("deleted a console proxy[vmUuid:%s, host IP: %s, host port: %s, proxy IP: %s, proxy port: %s",
                            vm.getUuid(), vo.getTargetHostname(), vo.getTargetPort(), vo.getProxyHostname(), vo.getProxyPort()));
                    completion.success();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    DeleteConsoleProxyGcJob gc = new DeleteConsoleProxyGcJob();
                    gc.NAME = String.format("delete-console-proxy-%s", vo.getUuid());
                    gc.consoleProxy = ConsoleProxyInventory.valueOf(vo);
                    gc.submit(ConsoleGlobalConfig.DELETE_CONSOLE_PROXY_RETRY_DELAY.value(Long.class), TimeUnit.SECONDS);
                    dbf.remove(vo);
                    completion.fail(errorCode);
                }
            });
        } else {
            completion.success();
        }
    }


    private void deploySaltState() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        asf.deployModule("ansible/consoleproxy", ANSIBLE_PLAYBOOK_NAME);
    }

    @Override
    public boolean start() {
        deploySaltState();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    @AsyncThread
    public void managementNodeReady() {
        connectAgent();
    }
}
