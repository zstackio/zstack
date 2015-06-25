package org.zstack.console;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.Component;
import org.zstack.header.console.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 7:32 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractConsoleProxyBackend implements ConsoleBackend, Component, ManagementNodeChangeListener {
    private static final CLogger logger = Utils.getLogger(AbstractConsoleProxyBackend.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected RESTFacade restf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected AnsibleFacade asf;

    protected static final String ANSIBLE_PLAYBOOK_NAME = "consoleproxy.yaml";

    protected abstract ConsoleProxy getConsoleProxy(VmInstanceInventory vm, ConsoleProxyVO vo);
    protected abstract ConsoleProxy getConsoleProxy(SessionInventory session, VmInstanceInventory vm);
    protected abstract void connectAgent();

    private void establishNewProxy(ConsoleProxy proxy, SessionInventory session, final VmInstanceInventory vm, final ReturnValueCompletion<ConsoleInventory> complete) {
        proxy.establishProxy(session, vm, new ReturnValueCompletion<ConsoleProxyInventory>() {
            @Override
            public void success(ConsoleProxyInventory ret) {
                ConsoleProxyVO vo = new ConsoleProxyVO();
                vo.setAgentIp(ret.getAgentIp());
                vo.setProxyIdentity(ret.getProxyIdentity());
                vo.setScheme(ret.getScheme());
                vo.setProxyHostname(ret.getProxyHostname());
                vo.setProxyPort(ret.getProxyPort());
                vo.setTargetHostname(ret.getTargetHostname());
                vo.setTargetPort(ret.getTargetPort());
                vo.setToken(ret.getToken());
                vo.setVmInstanceUuid(vm.getUuid());
                vo.setUuid(Platform.getUuid());
                vo.setAgentType(ret.getAgentType());
                vo.setStatus(ConsoleProxyStatus.Active);
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
        SimpleQuery<ConsoleProxyVO> q = dbf.createQuery(ConsoleProxyVO.class);
        q.add(ConsoleProxyVO_.vmInstanceUuid, SimpleQuery.Op.EQ, vm.getUuid());
        q.add(ConsoleProxyVO_.status, SimpleQuery.Op.EQ, ConsoleProxyStatus.Active);
        final ConsoleProxyVO vo = q.find();
        if (vo != null) {
            final ConsoleProxy proxy = getConsoleProxy(vm, vo);
            proxy.checkAvailability(new ReturnValueCompletion<Boolean>() {
                @Override
                public void success(Boolean returnValue) {
                    if (returnValue) {
                        ConsoleInventory retInv = ConsoleInventory.valueOf(vo);
                        complete.success(retInv);
                    } else {
                        //TODO: run cleanup on agent side
                        vo.setStatus(ConsoleProxyStatus.Inactive);
                        dbf.update(vo);

                        establishNewProxy(proxy, session, vm, complete);
                    }
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    complete.fail(errorCode);
                }
            });

            return;
        }

        ConsoleProxy proxy = getConsoleProxy(session, vm);
        establishNewProxy(proxy, session, vm, complete);
    }

    @Override
    public void deleteConsoleSession(VmInstanceInventory vm, Completion completion) {
        SimpleQuery<ConsoleProxyVO> q = dbf.createQuery(ConsoleProxyVO.class);
        q.add(ConsoleProxyVO_.vmInstanceUuid, SimpleQuery.Op.EQ, vm.getUuid());
        q.add(ConsoleProxyVO_.status, SimpleQuery.Op.EQ, ConsoleProxyStatus.Active);
        final ConsoleProxyVO vo = q.find();
        if (vo != null) {
            ConsoleProxy proxy = getConsoleProxy(vm, vo);
            proxy.deleteProxy(vm, completion);
            dbf.remove(vo);
            logger.debug(String.format("deleted a console proxy[vmUuid:%s, host IP: %s, host port: %s, proxy IP: %s, proxy port: %s",
                    vm.getUuid(), vo.getTargetHostname(), vo.getTargetPort(), vo.getProxyHostname(), vo.getProxyPort()));
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
    public void nodeJoin(String nodeId) {
    }

    @Override
    public void nodeLeft(String nodeId) {
    }

    @Override
    public void iAmDead(String nodeId) {
    }

    @Override
    @AsyncThread
    public void iJoin(String nodeId) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        connectAgent();
    }
}
