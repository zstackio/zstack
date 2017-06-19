package org.zstack.console;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleConstant;
import org.zstack.core.ansible.AnsibleGlobalProperty;
import org.zstack.core.ansible.AnsibleRunner;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.console.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.managementnode.ManagementNodeVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.URLBuilder;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import static org.zstack.core.Platform.argerr;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class ManagementServerConsoleProxyBackend extends AbstractConsoleProxyBackend {
    private static final CLogger logger = Utils.getLogger(ManagementServerConsoleProxyBackend.class);
    private int agentPort = 7758;
    private String agentPackageName = ConsoleGlobalProperty.AGENT_PACKAGE_NAME;
    private boolean connected = false;

    public static ConsoleProxyAgentType type = new ConsoleProxyAgentType(ConsoleConstants.MANAGEMENT_SERVER_CONSOLE_PROXY_TYPE);

    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ConsoleProxyAgentTracker tracker;

    protected ConsoleProxy getConsoleProxy(VmInstanceInventory vm, ConsoleProxyVO vo) {
        return new ConsoleProxyBase(vo, getAgentPort());
    }

    @Override
    protected ConsoleProxy getConsoleProxy(SessionInventory session, VmInstanceInventory vm) {
        String mgmtIp = CoreGlobalProperty.UNIT_TEST_ON ? "127.0.0.1" : Platform.getManagementServerIp();
        ConsoleProxyInventory inv = new ConsoleProxyInventory();
        inv.setScheme("http");
        inv.setProxyHostname(mgmtIp);
        inv.setAgentIp("127.0.0.1");
        inv.setAgentType(getConsoleBackendType());
        inv.setToken(session.getUuid() + "_" + vm.getUuid());
        inv.setVmInstanceUuid(vm.getUuid());
        return new ConsoleProxyBase(inv, getAgentPort());
    }

    private void setupPublicKey() throws IOException {
        File pubKeyFile = PathUtil.findFileOnClassPath(AnsibleConstant.RSA_PUBLIC_KEY);
        String script = PathUtil.findFileOnClassPath(AnsibleConstant.IMPORT_PUBLIC_KEY_SCRIPT_PATH, true).getAbsolutePath();

        ShellUtils.run(String.format("sh %s '%s'", script, pubKeyFile.getAbsolutePath()));
    }

    protected void doConnectAgent(final Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return String.format("deploy-console-agent-%s", Platform.getManagementServerId());
            }

            @Override
            @Deferred
            public void run(final SyncTaskChain chain) {
                ConsoleProxyAgentVO vo = dbf.findByUuid(Platform.getManagementServerId(), ConsoleProxyAgentVO.class);
                if (vo == null) {
                    vo = new ConsoleProxyAgentVO();
                    vo.setManagementIp(Platform.getManagementServerIp());
                    vo.setUuid(Platform.getManagementServerId());
                    vo.setConsoleProxyOverriddenIp(CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IP);
                    vo.setState(ConsoleProxyAgentState.Enabled);
                    vo.setStatus(ConsoleProxyAgentStatus.Connecting);
                    vo.setDescription(String.format("Console proxy agent running on the management node[uuid:%s]", Platform.getManagementServerId()));
                    vo.setType(ConsoleConstants.MANAGEMENT_SERVER_CONSOLE_PROXY_TYPE);
                    vo = dbf.persistAndRefresh(vo);
                }

                final ConsoleProxyAgentVO finalVo = vo;
                Defer.guard(new Runnable() {
                    @Override
                    public void run() {
                        finalVo.setStatus(ConsoleProxyAgentStatus.Disconnected);
                        dbf.update(finalVo);
                    }
                });

                try {
                    ShellUtils.run("rm -rf /var/lib/zstack/consoleProxy/*");

                    setupPublicKey();
                    File privKeyFile = PathUtil.findFileOnClassPath("ansible/rsaKeys/id_rsa");
                    String privKey = FileUtils.readFileToString(privKeyFile);

                    String srcPath = PathUtil.findFileOnClassPath(String.format("ansible/consoleproxy/%s", agentPackageName), true).getAbsolutePath();
                    String destPath = String.format("/var/lib/zstack/console/package/%s", agentPackageName);
                    SshFileMd5Checker checker = new SshFileMd5Checker();
                    checker.setTargetIp("127.0.0.1");
                    checker.setUsername("root");
                    checker.setPrivateKey(privKey);
                    checker.addSrcDestPair(SshFileMd5Checker.ZSTACKLIB_SRC_PATH, String.format("/var/lib/zstack/console/package/%s", AnsibleGlobalProperty.ZSTACKLIB_PACKAGE_NAME));
                    checker.addSrcDestPair(srcPath, destPath);

                    AnsibleRunner runner = new AnsibleRunner();
                    runner.setRunOnLocal(true);
                    runner.setLocalPublicKey(true);
                    runner.installChecker(checker);
                    runner.setUsername("root");
                    runner.setPrivateKey(privKey);
                    runner.setAgentPort(7758);
                    runner.setTargetIp(Platform.getManagementServerIp());
                    runner.setPlayBookName(ANSIBLE_PLAYBOOK_NAME);
                    runner.putArgument("pkg_consoleproxy", agentPackageName);
                    runner.run(new Completion(completion, chain) {
                        @Override
                        public void success() {
                            finalVo.setStatus(ConsoleProxyAgentStatus.Connected);
                            dbf.update(finalVo);

                            connected = true;
                            logger.debug("successfully deploy console proxy agent by ansible");
                            completion.success();
                            chain.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            finalVo.setStatus(ConsoleProxyAgentStatus.Disconnected);
                            dbf.update(finalVo);

                            connected = false;
                            logger.warn(String.format("failed to deploy console proxy agent by ansible, %s", errorCode));
                            completion.fail(errorCode);
                            chain.next();
                        }
                    });
                } catch (IOException e) {
                    throw new CloudRuntimeException(e);
                }
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    @Override
    @AsyncThread
    protected void connectAgent() {
        doConnectAgent(new NopeCompletion());
    }

    @Override
    protected boolean isAgentConnected() {
        return connected;
    }

    @Override
    public String getConsoleBackendType() {
        return ConsoleConstants.MANAGEMENT_SERVER_CONSOLE_PROXY_BACKEND;
    }

    @Override
    public String returnServiceIdForConsoleAgentMsg(ConsoleProxyAgentMessage msg, String agentUuid) {
        return bus.makeServiceIdByManagementNodeId(ConsoleConstants.SERVICE_ID, Platform.getManagementServerId());
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }

    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof ReconnectConsoleProxyMsg) {
            handle((ReconnectConsoleProxyMsg) msg);
        } else if (msg instanceof PingConsoleProxyAgentMsg) {
            handle((PingConsoleProxyAgentMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(PingConsoleProxyAgentMsg msg) {
        ConsoleProxyCommands.PingCmd cmd = new ConsoleProxyCommands.PingCmd();
        String url = URLBuilder.buildHttpUrl("127.0.0.1", agentPort, ConsoleConstants.CONSOLE_PROXY_PING_PATH);
        ConsoleProxyAgentVO vo = dbf.findByUuid(Platform.getManagementServerId(), ConsoleProxyAgentVO.class);

        boolean success;
        boolean reconnect = false;
        try {
            restf.syncJsonPost(url, cmd, ConsoleProxyCommands.PingRsp.class);
            success = true;
            if (vo != null) {
                reconnect = vo.getStatus() == ConsoleProxyAgentStatus.Disconnected;

                if (vo.getStatus() != ConsoleProxyAgentStatus.Connected) {
                    vo.setStatus(ConsoleProxyAgentStatus.Connected);
                    dbf.update(vo);
                }
            } else {
                reconnect = true;
            }

        } catch (Exception e) {
            logger.warn(String.format("cannot ping console proxy agent, %s", e.getMessage()), e);

            if (vo != null) {
                vo.setStatus(ConsoleProxyAgentStatus.Disconnected);
                dbf.update(vo);
            }
            success = false;
        }

        PingConsoleProxyAgentReply reply = new PingConsoleProxyAgentReply();
        reply.setConnected(success);
        reply.setDoReconnect(reconnect);
        bus.reply(msg, reply);
    }

    private void handle(final ReconnectConsoleProxyMsg msg) {
        final ReconnectConsoleProxyReply reply = new ReconnectConsoleProxyReply();
        doConnectAgent(new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIReconnectConsoleProxyAgentMsg) {
            handle((APIReconnectConsoleProxyAgentMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final APIReconnectConsoleProxyAgentMsg msg) {
        final APIReconnectConsoleProxyAgentEvent evt = new APIReconnectConsoleProxyAgentEvent(msg.getId());

        SimpleQuery<ManagementNodeVO> q = dbf.createQuery(ManagementNodeVO.class);
        q.select(ManagementNodeVO_.uuid);

        if (msg.getAgentUuids() != null) {
            q.add(ManagementNodeVO_.uuid, SimpleQuery.Op.IN, msg.getAgentUuids());
        }

        final List<String> mgmtNodeUuids = q.listValue();

        final Map<String, Object> errors = new HashMap<String, Object>();

        if (msg.getAgentUuids() != null) {
            for (String uuid : msg.getAgentUuids()) {
                if (!mgmtNodeUuids.contains(uuid)) {
                    errors.put(uuid, argerr("invalid management node UUID[%s]", uuid));
                }
            }
        }

        if (mgmtNodeUuids.isEmpty()) {
            evt.setInventory(errors);
            bus.publish(evt);
            return;
        }

        final List<ReconnectConsoleProxyMsg> rmsgs = CollectionUtils.transformToList(mgmtNodeUuids, new Function<ReconnectConsoleProxyMsg, String>() {
            @Override
            public ReconnectConsoleProxyMsg call(String arg) {
                ReconnectConsoleProxyMsg rmsg = new ReconnectConsoleProxyMsg();
                rmsg.setAgentUuid(arg);
                bus.makeServiceIdByManagementNodeId(rmsg, ConsoleConstants.SERVICE_ID, arg);
                return rmsg;
            }
        });

        bus.send(rmsgs, new CloudBusListCallBack(msg) {
            @Override
            public void run(List<MessageReply> replies) {
                for (MessageReply r : replies) {
                    String mgmgUuid = mgmtNodeUuids.get(replies.indexOf(r));
                    if (r.isSuccess()) {
                        errors.put(mgmgUuid, true);
                    } else {
                        errors.put(mgmgUuid, r.getError());
                    }
                }

                evt.setInventory(errors);

                bus.publish(evt);
            }
        });
    }

    @Override
    public boolean start() {
        tracker.track(Platform.getManagementServerId());
        return super.start();
    }


    public int getAgentPort() {
        return agentPort;
    }

    public void setAgentPort(int agentPort) {
        this.agentPort = agentPort;
    }
}
