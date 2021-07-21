package org.zstack.storage.ceph.backup;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.*;
import org.zstack.core.cloudbus.CloudBusGlobalProperty;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.storage.ceph.*;
import org.zstack.storage.ceph.backup.CephBackupStorageBase.PingOperationFailure;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshException;

import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * Created by frank on 7/27/2015.
 */

public class CephBackupStorageMonBase extends CephMonBase {
    private static final CLogger logger = Utils.getLogger(CephBackupStorageMonBase.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;

    private String syncId;

    public static final String ECHO_PATH = "/ceph/backupstorage/echo";
    public static final String PING_PATH = "/ceph/backupstorage/ping";
    public static final String EXPORT = "/ceph/export";

    public static class AgentCmd {
        public String monUuid;
        public String backupStorageUuid;
    }

    public static class AgentRsp {
        public boolean success;
        public String error;
    }

    public static class PingCmd extends AgentCmd {
        public String testImagePath;
        public String monAddr;
    }

    public static class PingRsp extends AgentRsp {
        public PingOperationFailure failure;
    }

    public CephBackupStorageMonBase(CephMonAO self) {
        super(self);
        syncId = String.format("ceph-backup-storage-mon-%s", getSelf().getUuid());
    }

    public CephBackupStorageMonVO getSelf() {
        return (CephBackupStorageMonVO) self;
    }

    public void changeStatus(MonStatus status) {
        String uuid = self.getUuid();
        self = dbf.reload(self);
        if (self == null) {
            throw new OperationFailureException(
                    operr("cannot update status of the ceph backup storage mon[uuid:%s], it has been deleted." +
                            "This error can be ignored", uuid)
            );
        }

        if (self.getStatus() == status) {
            return;
        }

        MonStatus oldStatus = self.getStatus();
        self.setStatus(status);
        self = dbf.updateAndRefresh(self);
        logger.debug(String.format("ceph backup storage mon[uuid:%s] changed status from %s to %s",
                self.getUuid(), oldStatus, status));
    }

    @Override
    public void connect(final Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return syncId;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                doConnect(new Completion(completion, chain) {
                    @Override
                    public void success() {
                        completion.success();
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("connect-ceph-backup-storage-mon-%s", self.getUuid());
            }
        });
    }

    private void doConnect(final Completion completion) {
        changeStatus(MonStatus.Connecting);

        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("connect-mon-%s-ceph-backup-storage-%s", self.getHostname(), getSelf().getBackupStorageUuid()));
        chain.allowEmptyFlow();
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                if (!CoreGlobalProperty.UNIT_TEST_ON) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "check-tools";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            checkTools();
                            trigger.next();
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "deploy-agent";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            SshFileMd5Checker checker = new SshFileMd5Checker();
                            checker.setTargetIp(getSelf().getHostname());
                            checker.setUsername(getSelf().getSshUsername());
                            checker.setPassword(getSelf().getSshPassword());
                            checker.addSrcDestPair(SshFileMd5Checker.ZSTACKLIB_SRC_PATH, String.format("/var/lib/zstack/cephb/package/%s", AnsibleGlobalProperty.ZSTACKLIB_PACKAGE_NAME));
                            checker.addSrcDestPair(PathUtil.findFileOnClassPath(String.format("ansible/cephb/%s", CephGlobalProperty.BACKUP_STORAGE_PACKAGE_NAME), true).getAbsolutePath(),
                                    String.format("/var/lib/zstack/cephb/package/%s", CephGlobalProperty.BACKUP_STORAGE_PACKAGE_NAME));

                            SshChronyConfigChecker chronyChecker = new SshChronyConfigChecker();
                            chronyChecker.setTargetIp(getSelf().getHostname());
                            chronyChecker.setUsername(getSelf().getSshUsername());
                            chronyChecker.setPassword(getSelf().getSshPassword());
                            chronyChecker.setSshPort(getSelf().getSshPort());

                            SshYumRepoChecker repoChecker = new SshYumRepoChecker();
                            repoChecker.setTargetIp(self.getHostname());
                            repoChecker.setUsername(self.getSshUsername());
                            repoChecker.setPassword(self.getSshPassword());
                            repoChecker.setSshPort(self.getSshPort());

                            CallBackNetworkChecker callbackChecker = new CallBackNetworkChecker();
                            callbackChecker.setTargetIp(getSelf().getHostname());
                            callbackChecker.setUsername(getSelf().getSshUsername());
                            callbackChecker.setPassword(getSelf().getSshPassword());
                            callbackChecker.setPort(getSelf().getSshPort());
                            callbackChecker.setCallbackIp(Platform.getManagementServerIp());
                            callbackChecker.setCallBackPort(CloudBusGlobalProperty.HTTP_PORT);

                            AnsibleRunner runner = new AnsibleRunner();
                            runner.installChecker(checker);
                            runner.installChecker(chronyChecker);
                            runner.installChecker(repoChecker);
                            runner.installChecker(callbackChecker);
                            runner.setPassword(getSelf().getSshPassword());
                            runner.setUsername(getSelf().getSshUsername());
                            runner.setTargetIp(getSelf().getHostname());
                            runner.setTargetUuid(getSelf().getUuid());
                            runner.setSshPort(getSelf().getSshPort());
                            runner.setAgentPort(CephGlobalProperty.BACKUP_STORAGE_AGENT_PORT);
                            runner.setPlayBookName(CephGlobalProperty.BACKUP_STORAGE_PLAYBOOK_NAME);
                            runner.putArgument("pkg_cephbagent", CephGlobalProperty.BACKUP_STORAGE_PACKAGE_NAME);
                            if (CoreGlobalProperty.SYNC_NODE_TIME) {
                                if (CoreGlobalProperty.CHRONY_SERVERS == null || CoreGlobalProperty.CHRONY_SERVERS.isEmpty()) {
                                    trigger.fail(operr("chrony server not configured!"));
                                    return;
                                }
                                runner.putArgument("chrony_servers", String.join(",", CoreGlobalProperty.CHRONY_SERVERS));
                            }
                            runner.run(new ReturnValueCompletion<Boolean>(trigger) {
                                @Override
                                public void success(Boolean deployed) {
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
                        String __name__ = "deploy-more-agent-to-backupStorage";
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            List<CephMonExtensionPoint> exts = pluginRgty.getExtensionList(CephMonExtensionPoint.class);
                            FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
                            chain.allowEmptyFlow();
                            for(CephMonExtensionPoint ext: exts) {
                                chain.then(new NoRollbackFlow() {
                                    @Override
                                    public void run(FlowTrigger trigger1, Map data) {
                                        ext.addMoreAgentInBackupStorage(CephBackupStorageMonInventory.valueOf(getSelf()), new Completion(trigger1) {
                                            @Override
                                            public void success() {
                                                trigger1.next();
                                            }

                                            @Override
                                            public void fail(ErrorCode errorCode) {
                                                trigger1.fail(errorCode);
                                            }
                                        });
                                    }
                                });
                            }
                            chain.done(new FlowDoneHandler(trigger) {
                                @Override
                                public void handle(Map data) {
                                    trigger.next();
                                }
                            }).error(new FlowErrorHandler(trigger) {
                                @Override
                                public void handle(ErrorCode errCode, Map data) {
                                    trigger.fail(errCode);
                                }
                            }).start();
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "configure-iptables";

                        @Autowired
                        public void run(FlowTrigger trigger, Map data) {

                            StringBuilder builder = new StringBuilder();
                            if (!CephGlobalProperty.MN_NETWORKS.isEmpty()) {
                                builder.append(String.format("sudo bash %s -m %s -p %s -c %s",
                                        "/var/lib/zstack/cephb/package/cephbs-iptables",
                                        CephConstants.CEPH_BS_IPTABLES_COMMENTS,
                                        CephGlobalConfig.CEPH_BS_ALLOW_PORTS.value(),
                                        String.join(",", CephGlobalProperty.MN_NETWORKS)));
                            } else {
                                builder.append(String.format("sudo bash %s -m %s -p %s",
                                        "/var/lib/zstack/cephb/package/cephbs-iptables",
                                        CephConstants.CEPH_BS_IPTABLES_COMMENTS,
                                        CephGlobalConfig.CEPH_BS_ALLOW_PORTS.value()));
                            }
                            try {
                                new Ssh().shell(builder.toString())
                                        .setUsername(self.getSshUsername())
                                        .setPassword(self.getSshPassword())
                                        .setHostname(self.getHostname())
                                        .setPort(self.getSshPort()).runErrorByExceptionAndClose();
                            } catch (SshException ex) {
                                throw new OperationFailureException(operr(ex.toString()));
                            }

                            trigger.next();
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "echo-agent";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            restf.echo(CephAgentUrl.backupStorageUrl(getSelf().getHostname(), ECHO_PATH), new Completion(trigger) {
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
                }

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        changeStatus(MonStatus.Connected);
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        changeStatus(MonStatus.Disconnected);
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void httpCall(String path, AgentCmd cmd, final ReturnValueCompletion<AgentRsp> completion) {
        httpCall(path, cmd, AgentRsp.class, completion);
    }

    private <T> void httpCall(String path, AgentCmd cmd, final Class<T> rspClass, final ReturnValueCompletion<T> completion) {
        restf.asyncJsonPost(CephAgentUrl.backupStorageUrl(self.getHostname(), path),
                cmd, new JsonAsyncRESTCallback<T>(completion) {
                    @Override
                    public void fail(ErrorCode err) {
                        completion.fail(err);
                    }

                    @Override
                    public void success(T ret) {
                        completion.success(ret);
                    }

                    @Override
                    public Class<T> getReturnClass() {
                        return rspClass;
                    }
                });
    }

    @Override
    public void ping(final ReturnValueCompletion<PingResult> completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return syncId;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                doPing(new ReturnValueCompletion<PingResult>(completion, chain) {
                    @Override
                    public void success(PingResult ret) {
                        completion.success(ret);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("ping-ceph-backup-storage-mon-%s", self.getUuid());
            }
        });
    }

    @Override
    protected int getAgentPort() {
        return CephGlobalProperty.BACKUP_STORAGE_AGENT_PORT;
    }

    @Override
    protected String makeHttpPath(String ip, String path) {
        return CephAgentUrl.backupStorageUrl(ip, path);
    }

    public void doPing(final ReturnValueCompletion<PingResult> completion) {
        SimpleQuery<CephBackupStorageVO> q = dbf.createQuery(CephBackupStorageVO.class);
        q.select(CephBackupStorageVO_.poolName);
        q.add(CephBackupStorageVO_.uuid, Op.EQ, getSelf().getBackupStorageUuid());
        String poolName = q.findValue();
        if (poolName == null) {
            completion.fail(operr("Ceph bs[uuid=%s] pool name not found", getSelf().getBackupStorageUuid()));
            return;
        }

        PingCmd cmd = new PingCmd();
        cmd.monAddr = String.format("%s:%s", getSelf().getMonAddr(), getSelf().getMonPort());
        cmd.testImagePath = String.format("%s/zshb.bs.%s.%s", poolName, self.getUuid(), self.getMonAddr());
        cmd.monUuid = getSelf().getUuid();
        cmd.backupStorageUuid = getSelf().getBackupStorageUuid();

        httpCall(PING_PATH, cmd, PingRsp.class, new ReturnValueCompletion<PingRsp>(completion) {
            @Override
            public void success(PingRsp rsp) {
                PingResult res = new PingResult();
                if (rsp.success) {
                    res.success = true;
                } else {
                    res.success = false;
                    res.error = rsp.error;
                    res.failure = rsp.failure.toString();
                }

                completion.success(res);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }
}
