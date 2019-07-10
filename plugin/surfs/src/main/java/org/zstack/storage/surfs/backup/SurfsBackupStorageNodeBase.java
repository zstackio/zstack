package org.zstack.storage.surfs.backup;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleGlobalProperty;
import org.zstack.core.ansible.AnsibleRunner;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.storage.surfs.SurfsGlobalProperty;
import org.zstack.storage.surfs.SurfsNodeAO;
import org.zstack.storage.surfs.SurfsNodeBase;
import org.zstack.storage.surfs.NodeStatus;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.util.Map;

/**
 * Created by zhouhaiping 2017-09-07
 */

public class SurfsBackupStorageNodeBase extends SurfsNodeBase {
    private static final CLogger logger = Utils.getLogger(SurfsBackupStorageNodeBase.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;

    private String syncId;

    public static final String ECHO_PATH = "/surfs/backupstorage/echo";
    public static final String PING_PATH = "/surfs/backupstorage/ping";

    public static class AgentCmd {
        public String NodeUuid;
        public String backupStorageUuid;
    }

    public static class AgentRsp {
        public boolean success;
        public String error;
    }

    public static class PingCmd extends AgentCmd {
        public String testImagePath;
    }

    public static class PingRsp extends AgentRsp {
        public boolean operationFailure;
    }

    public SurfsBackupStorageNodeBase(SurfsNodeAO self) {
        super(self);
        syncId = String.format("surfs-backup-storage-node-%s", getSelf().getUuid());
    }

    public SurfsBackupStorageNodeVO getSelf() {
        return (SurfsBackupStorageNodeVO) self;
    }

    public void changeStatus(NodeStatus status) {
        if (self.getStatus() == status) {
            return;
        }

        NodeStatus oldStatus = self.getStatus();
        self.setStatus(status);
        self = dbf.updateAndRefresh(self);
        logger.debug(String.format("surfs backup storage node[uuid:%s] changed status from %s to %s",
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
                return String.format("connect-surfs-backup-storage-node-%s", self.getUuid());
            }
        });
    }

    private void doConnect(final Completion completion) {
        changeStatus(NodeStatus.Connecting);

        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("connect-node-%s-surfs-backup-storage-%s", self.getHostname(), getSelf().getBackupStorageUuid()));
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
                            checker.addSrcDestPair(SshFileMd5Checker.ZSTACKLIB_SRC_PATH, String.format("/var/lib/zstack/surfsb/package/%s", AnsibleGlobalProperty.ZSTACKLIB_PACKAGE_NAME));
                            checker.addSrcDestPair(PathUtil.findFileOnClassPath(String.format("ansible/surfsb/%s", SurfsGlobalProperty.BACKUP_STORAGE_PACKAGE_NAME), true).getAbsolutePath(),
                                    String.format("/var/lib/zstack/surfsb/package/%s", SurfsGlobalProperty.BACKUP_STORAGE_PACKAGE_NAME));
                            AnsibleRunner runner = new AnsibleRunner();
                            runner.installChecker(checker);
                            runner.setPassword(getSelf().getSshPassword());
                            runner.setUsername(getSelf().getSshUsername());
                            runner.setTargetIp(getSelf().getHostname());
                            runner.setSshPort(getSelf().getSshPort());
                            runner.setAgentPort(SurfsGlobalProperty.BACKUP_STORAGE_AGENT_PORT);
                            runner.setPlayBookName(SurfsGlobalProperty.BACKUP_STORAGE_PLAYBOOK_NAME);
                            runner.putArgument("pkg_surfsbagent", SurfsGlobalProperty.BACKUP_STORAGE_PACKAGE_NAME);
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

                    flow(new NoRollbackFlow() {
                        String __name__ = "echo-agent";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            restf.echo(String.format("http://%s:%s%s", getSelf().getHostname(),
                                    SurfsGlobalProperty.BACKUP_STORAGE_AGENT_PORT, ECHO_PATH), new Completion(trigger) {
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
                        changeStatus(NodeStatus.Connected);
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        changeStatus(NodeStatus.Disconnected);
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
        restf.asyncJsonPost(String.format("http://%s:%s%s", self.getHostname(), SurfsGlobalProperty.BACKUP_STORAGE_AGENT_PORT, path),
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
                return String.format("ping-surfs-backup-storage-node-%s", self.getUuid());
            }
        });
    }

    @Override
    protected int getAgentPort() {
        return SurfsGlobalProperty.BACKUP_STORAGE_AGENT_PORT;
    }

    public void doPing(final ReturnValueCompletion<PingResult> completion) {
        SimpleQuery<SurfsBackupStorageVO> q = dbf.createQuery(SurfsBackupStorageVO.class);
        q.select(SurfsBackupStorageVO_.poolName);
        q.add(SurfsBackupStorageVO_.uuid, Op.EQ, getSelf().getBackupStorageUuid());
        String poolName = q.findValue();

        PingCmd cmd = new PingCmd();
        cmd.testImagePath = String.format("%s/%s-this-is-a-test-image-with-long-name", poolName, Platform.getUuid());
        cmd.NodeUuid = getSelf().getUuid();
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
                    res.operationFailure = rsp.operationFailure;
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
