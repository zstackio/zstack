package org.zstack.console;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.GlobalProperty;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.console.*;
import org.zstack.header.console.ConsoleProxyCommands.DeleteProxyCmd;
import org.zstack.header.console.ConsoleProxyCommands.DeleteProxyRsp;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.utils.URLBuilder;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:07 PM
 * To change this template use File | Settings | File Templates.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ConsoleProxyBase implements ConsoleProxy {
    private static final CLogger logger = Utils.getLogger(ConsoleProxyBase.class);
    private ConsoleProxyInventory self;

    @Autowired
    private RESTFacade restf;
    @Autowired
    private ConsoleManager consoleMgr;
    @Autowired
    private ErrorFacade errf;

    private final int agentPort;

    public ConsoleProxyBase(ConsoleProxyVO vo, int agentPort) {
        self = ConsoleProxyInventory.valueOf(vo);
        this.agentPort = agentPort;
    }

    public ConsoleProxyBase(ConsoleProxyInventory inv, int agentPort) {
        self = inv;
        this.agentPort = agentPort;
    }

    private void doEstablish(URI uri, final ReturnValueCompletion<ConsoleProxyInventory> completion) {
        final String targetHostname = uri.getHost();
        final int targetPort = uri.getPort();

        int timeout = ConsoleGlobalConfig.PROXY_IDLE_TIMEOUT.value(Integer.class);

        ConsoleProxyCommands.EstablishProxyCmd cmd = new ConsoleProxyCommands.EstablishProxyCmd();
        cmd.setVmUuid(self.getVmInstanceUuid());
        cmd.setTargetHostname(targetHostname);
        cmd.setTargetPort(targetPort);
        cmd.setProxyHostname("0.0.0.0");
        cmd.setProxyPort(CoreGlobalProperty.CONSOLE_PROXY_PORT);
        cmd.setScheme(self.getScheme());
        cmd.setToken(self.getToken());
        cmd.setIdleTimeout(timeout);

        String agentUrl = URLBuilder.buildHttpUrl(self.getAgentIp(), agentPort, ConsoleConstants.CONSOLE_PROXY_ESTABLISH_PROXY_PATH);
        restf.asyncJsonPost(agentUrl, cmd, new JsonAsyncRESTCallback<ConsoleProxyCommands.EstablishProxyRsp>(completion) {
            @Override
            public void fail(ErrorCode err) {
                completion.fail(err);
            }

            @Override
            public void success(ConsoleProxyCommands.EstablishProxyRsp ret) {
                if (ret.isSuccess()) {
                    self.setTargetHostname(targetHostname);
                    self.setTargetPort(targetPort);
                    self.setProxyPort(ret.getProxyPort());
                    completion.success(self);
                } else {
                    completion.fail(operr(ret.getError()));
                }
            }

            @Override
            public Class<ConsoleProxyCommands.EstablishProxyRsp> getReturnClass() {
                return ConsoleProxyCommands.EstablishProxyRsp.class;
            }
        });
    }

    @Override
    public void establishProxy(final SessionInventory session, final VmInstanceInventory vm, final ReturnValueCompletion<ConsoleProxyInventory> completion) {
        ConsoleHypervisorBackend bkd = consoleMgr.getHypervisorConsoleBackend(HypervisorType.valueOf(vm.getHypervisorType()));
        bkd.generateConsoleUrl(vm, new ReturnValueCompletion<URI>(completion) {

            @Override
            public void success(URI uri) {
                doEstablish(uri, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });

    }

    @Override
    public void checkAvailability(final ReturnValueCompletion<Boolean> completion) {
        ConsoleProxyCommands.CheckAvailabilityCmd cmd = new ConsoleProxyCommands.CheckAvailabilityCmd();
        cmd.setProxyHostname(self.getProxyHostname());
        cmd.setProxyPort(self.getProxyPort());
        cmd.setTargetHostname(self.getTargetHostname());
        cmd.setTargetPort(self.getTargetPort());
        cmd.setProxyIdentity(self.getProxyIdentity());
        cmd.setToken(self.getToken());
        cmd.setScheme(self.getScheme());
        restf.asyncJsonPost(URLBuilder.buildHttpUrl(self.getAgentIp(), agentPort, ConsoleConstants.CONSOLE_PROXY_CHECK_PROXY_PATH),
                cmd, new JsonAsyncRESTCallback<ConsoleProxyCommands.CheckAvailabilityRsp>(completion) {
            @Override
            public void fail(ErrorCode err) {
                completion.fail(err);
            }

            @Override
            public void success(ConsoleProxyCommands.CheckAvailabilityRsp ret) {
                if (ret.isSuccess()) {
                    completion.success(ret.getAvailable());
                } else {
                    completion.fail(operr("unable to check console proxy availability, because %s", ret.getError()));
                }
            }

            @Override
            public Class<ConsoleProxyCommands.CheckAvailabilityRsp> getReturnClass() {
                return ConsoleProxyCommands.CheckAvailabilityRsp.class;
            }

        });
    }

    @Override
    public void deleteProxy(VmInstanceInventory vm, final Completion completion) {
        DeleteProxyCmd cmd = new DeleteProxyCmd();
        cmd.setProxyHostname(self.getProxyHostname());
        cmd.setProxyPort(self.getProxyPort());
        cmd.setTargetHostname(self.getTargetHostname());
        cmd.setTargetPort(self.getTargetPort());
        cmd.setToken(self.getToken());
        cmd.setVmUuid(vm.getUuid());

        restf.asyncJsonPost(URLBuilder.buildHttpUrl(self.getAgentIp(), agentPort, ConsoleConstants.CONSOLE_PROXY_DELETE_PROXY_PATH), cmd,
                new JsonAsyncRESTCallback<DeleteProxyRsp>(completion) {
                    @Override
                    public void fail(ErrorCode err) {
                        completion.fail(err);
                    }

                    @Override
                    public void success(DeleteProxyRsp ret) {
                        if (ret.isSuccess()) {
                            completion.success();
                        } else {
                            completion.fail(operr(ret.getError()));
                        }
                    }

                    @Override
                    public Class<DeleteProxyRsp> getReturnClass() {
                        return DeleteProxyRsp.class;
                    }
                });
    }
}
