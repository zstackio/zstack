package org.zstack.storage.zbs;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.cbd.MdsInfo;
import org.zstack.cbd.MdsStatus;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.*;
import org.zstack.core.cloudbus.CloudBusGlobalProperty;
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
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshException;

import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * @author Xingwei Yu
 * @date 2024/4/2 11:48
 */
public class ZbsPrimaryStorageMdsBase extends ZbsMdsBase {
    private static final CLogger logger = Utils.getLogger(ZbsPrimaryStorageMdsBase.class);

    private String syncId;

    @Autowired
    private ThreadFacade thdf;

    public static final String ECHO_PATH = "/zbs/primarystorage/echo";

    public ZbsPrimaryStorageMdsBase(MdsInfo self) {
        super(self);
        this.syncId = String.format("connect-mds-%s", self.getMdsAddr());
    }

    private void doConnect(final Completion completion) {
        getSelf().setMdsStatus(MdsStatus.Connecting);

        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("connect-mds");
        chain.allowEmptyFlow();
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                if (!CoreGlobalProperty.UNIT_TEST_ON) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "check-mds";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            checkTools();
                            checkHealth();
                            trigger.next();
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "deploy-agent";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            SshFileMd5Checker checker = new SshFileMd5Checker();
                            checker.setTargetIp(getSelf().getMdsAddr());
                            checker.setUsername(getSelf().getSshUsername());
                            checker.setPassword(getSelf().getSshPassword());
                            checker.addSrcDestPair(SshFileMd5Checker.ZSTACKLIB_SRC_PATH, String.format("/var/lib/zstack/zbsp/package/%s", AnsibleGlobalProperty.ZSTACKLIB_PACKAGE_NAME));
                            checker.addSrcDestPair(PathUtil.findFileOnClassPath(String.format("ansible/zbsp/%s", ZbsGlobalProperty.PRIMARY_STORAGE_PACKAGE_NAME), true).getAbsolutePath(),
                                    String.format("/var/lib/zstack/zbsp/package/%s", ZbsGlobalProperty.PRIMARY_STORAGE_PACKAGE_NAME));

                            SshChronyConfigChecker chronyChecker = new SshChronyConfigChecker();
                            chronyChecker.setTargetIp(getSelf().getMdsAddr());
                            chronyChecker.setUsername(getSelf().getSshUsername());
                            chronyChecker.setPassword(getSelf().getSshPassword());
                            chronyChecker.setSshPort(getSelf().getSshPort());

                            SshYumRepoChecker repoChecker = new SshYumRepoChecker();
                            repoChecker.setTargetIp(getSelf().getMdsAddr());
                            repoChecker.setUsername(getSelf().getSshUsername());
                            repoChecker.setPassword(getSelf().getSshPassword());
                            repoChecker.setSshPort(getSelf().getSshPort());

                            CallBackNetworkChecker callBackChecker = new CallBackNetworkChecker();
                            callBackChecker.setTargetIp(getSelf().getMdsAddr());
                            callBackChecker.setUsername(getSelf().getSshUsername());
                            callBackChecker.setPassword(getSelf().getSshPassword());
                            callBackChecker.setPort(getSelf().getSshPort());
                            callBackChecker.setCallbackIp(Platform.getManagementServerIp());
                            callBackChecker.setCallBackPort(CloudBusGlobalProperty.HTTP_PORT);

                            AnsibleRunner runner = new AnsibleRunner();
                            runner.installChecker(checker);
                            runner.installChecker(chronyChecker);
                            runner.installChecker(repoChecker);
                            runner.installChecker(callBackChecker);
                            runner.setUsername(getSelf().getSshUsername());
                            runner.setPassword(getSelf().getSshPassword());
                            runner.setTargetIp(getSelf().getMdsAddr());
                            runner.setTargetUuid(getSelf().getMdsAddr());
                            runner.setAgentPort(ZbsGlobalProperty.PRIMARY_STORAGE_AGENT_PORT);
                            runner.setPlayBookName(ZbsGlobalProperty.PRIMARY_STORAGE_PLAYBOOK_NAME);

                            ZbsPrimaryStorageDeployArguments deployArguments = new ZbsPrimaryStorageDeployArguments();
                            runner.setDeployArguments(deployArguments);
                            runner.run(new ReturnValueCompletion<Boolean>(trigger) {
                                @Override
                                public void success(Boolean returnValue) {
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
                        String __name__ = "configure-iptables";

                        @Autowired
                        public void run(FlowTrigger trigger, Map data) {
                            StringBuilder builder = new StringBuilder();
                            if (!ZbsGlobalProperty.MN_NETWORKS.isEmpty()) {
                                builder.append(String.format("sudo bash %s -m %s -p %s -c %s",
                                        "/var/lib/zstack/zbsp/package/zbsps-iptables",
                                        ZbsConstants.ZBS_PS_IPTABLES_COMMENTS,
                                        ZbsGlobalConfig.ZBS_PS_ALLOW_PORTS.value(String.class),
                                        String.join(",", ZbsGlobalProperty.MN_NETWORKS)));
                            } else {
                                builder.append(String.format("sudo bash %s -m %s -p %s",
                                        "/var/lib/zstack/zbsp/package/zbsps-iptables",
                                        ZbsConstants.ZBS_PS_IPTABLES_COMMENTS,
                                        ZbsGlobalConfig.ZBS_PS_ALLOW_PORTS.value(String.class)));
                            }

                            try {
                                new Ssh().shell(builder.toString())
                                        .setUsername(getSelf().getSshUsername())
                                        .setPassword(getSelf().getSshPassword())
                                        .setHostname(getSelf().getMdsAddr())
                                        .setPort(getSelf().getSshPort()).runErrorByExceptionAndClose();
                            } catch (SshException ex) {
                                throw new OperationFailureException(operr(ex.toString()));
                            }

                            trigger.next();
                        }
                    });
                }

                flow(new NoRollbackFlow() {
                    String __name__ = "echo-agent";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        restf.echo(ZbsAgentUrl.primaryStorageUrl(getSelf().getMdsAddr(), ECHO_PATH), new Completion(trigger) {
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
                        getSelf().setMdsStatus(MdsStatus.Connected);
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        getSelf().setMdsStatus(MdsStatus.Disconnected);
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public void connect(Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public void run(SyncTaskChain chain) {
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
            public String getSyncSignature() {
                return syncId;
            }

            @Override
            public String getName() {
                return String.format("mds-%s", getSelf().getMdsAddr());
            }
        });
    }

    @Override
    protected String makeHttpPath(String ip, String path) {
        return ZbsAgentUrl.primaryStorageUrl(ip, path);
    }
}
