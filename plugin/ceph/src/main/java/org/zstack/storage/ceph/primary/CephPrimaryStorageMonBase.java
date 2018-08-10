package org.zstack.storage.ceph.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleGlobalProperty;
import org.zstack.core.ansible.AnsibleRunner;
import org.zstack.core.ansible.SshChronyConfigChecker;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
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
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * Created by frank on 7/28/2015.
 */
public class CephPrimaryStorageMonBase extends CephMonBase {
    private static final CLogger logger = Utils.getLogger(CephPrimaryStorageMonBase.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;

    private String syncId;

    public static final String ECHO_PATH = "/ceph/primarystorage/echo";
    public static final String PING_PATH = "/ceph/primarystorage/ping";

    public enum PingOperationFailure {
        UnableToCreateFile,
        MonAddrChanged,
    }


    public static class AgentCmd {
        public String monUuid;
        public String primaryStorageUuid;
    }

    public static class AgentRsp {
        public boolean success;
        public String error;
        public Long totalCapacity;
        public Long availableCapacity;
        public List<CephPoolCapacity> poolCapacities;
    }

    public static class PingCmd extends AgentCmd {
        public String testImagePath;
        public String monAddr;
    }

    public static class PingRsp extends AgentRsp {
        public boolean operationFailure;
        public PingOperationFailure failure;
    }

    public CephPrimaryStorageMonVO getSelf() {
        return (CephPrimaryStorageMonVO) self;
    }

    public void changeStatus(MonStatus status) {
        String uuid = self.getUuid();
        self = dbf.reload(self);
        if (self == null) {
            throw new OperationFailureException(operr(
                    "cannot update status of the ceph primary storage mon[uuid:%s], it has been deleted." +
                            "This error can be ignored", uuid
            ));
        }

        if (self.getStatus() == status) {
            return;
        }

        MonStatus oldStatus = self.getStatus();
        self.setStatus(status);
        self = dbf.updateAndRefresh(self);
        logger.debug(String.format("Ceph primary storage mon[uuid:%s] changed status from %s to %s", self.getUuid(), oldStatus, status));
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
                return String.format("connect-ceph-primary-storage-mon-%s", self.getUuid());
            }
        });
    }

    private void doConnect(final Completion completion) {
        changeStatus(MonStatus.Connecting);

        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("connect-mon-%s-ceph-primary-storage-%s", self.getHostname(), getSelf().getPrimaryStorageUuid()));
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
                            checker.addSrcDestPair(SshFileMd5Checker.ZSTACKLIB_SRC_PATH, String.format("/var/lib/zstack/cephp/package/%s", AnsibleGlobalProperty.ZSTACKLIB_PACKAGE_NAME));
                            checker.addSrcDestPair(PathUtil.findFileOnClassPath(String.format("ansible/cephp/%s", CephGlobalProperty.PRIMARY_STORAGE_PACKAGE_NAME), true).getAbsolutePath(),
                                    String.format("/var/lib/zstack/cephp/package/%s", CephGlobalProperty.PRIMARY_STORAGE_PACKAGE_NAME));

                            SshChronyConfigChecker chronyChecker = new SshChronyConfigChecker();
                            chronyChecker.setTargetIp(getSelf().getHostname());
                            chronyChecker.setUsername(getSelf().getSshUsername());
                            chronyChecker.setPassword(getSelf().getSshPassword());
                            chronyChecker.setSshPort(getSelf().getSshPort());

                            AnsibleRunner runner = new AnsibleRunner();
                            runner.installChecker(checker);
                            runner.installChecker(chronyChecker);
                            runner.setPassword(getSelf().getSshPassword());
                            runner.setUsername(getSelf().getSshUsername());
                            runner.setSshPort(getSelf().getSshPort());
                            runner.setTargetIp(getSelf().getHostname());
                            runner.setAgentPort(CephGlobalProperty.PRIMARY_STORAGE_AGENT_PORT);
                            runner.setPlayBookName(CephGlobalProperty.PRIMARY_STORAGE_PLAYBOOK_NAME);
                            runner.putArgument("pkg_cephpagent", CephGlobalProperty.PRIMARY_STORAGE_PACKAGE_NAME);
                            if (CoreGlobalProperty.SYNC_NODE_TIME) {
                                if (CoreGlobalProperty.CHRONY_SERVERS == null || CoreGlobalProperty.CHRONY_SERVERS.isEmpty()) {
                                    trigger.fail(operr("chrony server not configured!"));
                                    return;
                                }
                                runner.putArgument("chrony_servers", String.join(",", CoreGlobalProperty.CHRONY_SERVERS));
                            }
                            runner.run(new Completion(trigger) {
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

                flow(new NoRollbackFlow() {
                    String __name__ = "deploy-more-agent";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<CephMonExtensionPoint> exts = pluginRgty.getExtensionList(CephMonExtensionPoint.class);
                        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
                        for(CephMonExtensionPoint ext: exts) {
                            chain.then(new NoRollbackFlow() {
                                @Override
                                public void run(FlowTrigger trigger1, Map data) {
                                    ext.addMoreAgentInPrimaryStorage(CephPrimaryStorageMonInventory.valueOf(getSelf()), new Completion(trigger1) {
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
                        });
                        if (chain.getFlows().size() > 0) {
                            chain.start();
                        } else {
                            trigger.next();
                        }
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "echo-agent";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        restf.echo(CephAgentUrl.primaryStorageUrl(getSelf().getHostname(), ECHO_PATH), new Completion(trigger) {
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
        restf.asyncJsonPost(CephAgentUrl.primaryStorageUrl(self.getHostname(), path),
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
                doPing(new ReturnValueCompletion<PingResult>(completion) {
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
                return String.format("ping-ceph-primary-storage-%s", self.getUuid());
            }
        });
    }

    @Override
    protected int getAgentPort() {
        return CephGlobalProperty.PRIMARY_STORAGE_AGENT_PORT;
    }

    @Override
    protected String makeHttpPath(String ip, String path) {
        return CephAgentUrl.primaryStorageUrl(ip, path);
    }

    private void doPing(final ReturnValueCompletion<PingResult> completion) {
        String primaryStorageUuid = Q.New(CephPrimaryStorageMonVO.class)
                .select(CephPrimaryStorageMonVO_.primaryStorageUuid)
                .eq(CephPrimaryStorageMonVO_.uuid, self.getUuid())
                .findValue();
        String poolName = Q.New(CephPrimaryStoragePoolVO.class)
                .select(CephPrimaryStoragePoolVO_.poolName)
                .eq(CephPrimaryStoragePoolVO_.primaryStorageUuid, primaryStorageUuid)
                .eq(CephPrimaryStoragePoolVO_.type, CephPrimaryStoragePoolType.Root.toString())
                .findValue();

        PingCmd cmd = new PingCmd();
        cmd.testImagePath = String.format("%s/zshb.ps.%s.%s", poolName, self.getUuid(), self.getMonAddr());
        cmd.monUuid = getSelf().getUuid();
        cmd.primaryStorageUuid = getSelf().getPrimaryStorageUuid();
        cmd.monAddr = String.format("%s:%s", getSelf().getMonAddr(), getSelf().getMonPort());

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

                if (rsp.success && rsp.availableCapacity != null && rsp.totalCapacity != null) {
                    String fsid = Q.New(CephPrimaryStorageVO.class)
                            .select(CephPrimaryStorageVO_.fsid)
                            .eq(CephPrimaryStorageVO_.uuid, primaryStorageUuid)
                            .findValue();
                    new CephCapacityUpdater().update(fsid, rsp.totalCapacity, rsp.availableCapacity, rsp.poolCapacities);
                }

                completion.success(res);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    public CephPrimaryStorageMonBase(CephMonAO self) {
        super(self);
        syncId = String.format("ceph-primary-storage-mon-%s", self.getUuid());
    }
}
