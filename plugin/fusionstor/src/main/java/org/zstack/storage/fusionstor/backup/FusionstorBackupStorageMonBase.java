package org.zstack.storage.fusionstor.backup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleGlobalProperty;
import org.zstack.core.ansible.AnsibleRunner;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.rest.RESTFacade;
import org.zstack.storage.fusionstor.FusionstorGlobalProperty;
import org.zstack.storage.fusionstor.FusionstorMonAO;
import org.zstack.storage.fusionstor.FusionstorMonBase;
import org.zstack.storage.fusionstor.MonStatus;
import org.zstack.utils.path.PathUtil;

import java.util.Map;

/**
 * Created by frank on 7/27/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public class FusionstorBackupStorageMonBase extends FusionstorMonBase {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private RESTFacade restf;

    public static final String ECHO_PATH = "/fusionstor/backupstorage/echo";

    public FusionstorBackupStorageMonBase(FusionstorMonAO self) {
        super(self);
    }

    public FusionstorBackupStorageMonVO getSelf() {
        return (FusionstorBackupStorageMonVO) self;
    }

    public void connect(final Completion completion) {
        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("connect-mon-%s-fusionstor-backup-storage-%s", self.getHostname(), getSelf().getBackupStorageUuid()));
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
                            checker.addSrcDestPair(SshFileMd5Checker.ZSTACKLIB_SRC_PATH, String.format("/var/lib/zstack/fusionstorb/%s", AnsibleGlobalProperty.ZSTACKLIB_PACKAGE_NAME));
                            checker.addSrcDestPair(PathUtil.findFileOnClassPath(String.format("ansible/fusionstorb/%s", FusionstorGlobalProperty.BACKUP_STORAGE_PACKAGE_NAME), true).getAbsolutePath(),
                                    String.format("/var/lib/zstack/fusionstorb/%s", FusionstorGlobalProperty.BACKUP_STORAGE_PACKAGE_NAME));
                            AnsibleRunner runner = new AnsibleRunner();
                            runner.installChecker(checker);
                            runner.setPassword(getSelf().getSshPassword());
                            runner.setUsername(getSelf().getSshUsername());
                            runner.setTargetIp(getSelf().getHostname());
                            runner.setSshPort(getSelf().getSshPort());
                            runner.setAgentPort(FusionstorGlobalProperty.BACKUP_STORAGE_AGENT_PORT);
                            runner.setPlayBookName(FusionstorGlobalProperty.BACKUP_STORAGE_PLAYBOOK_NAME);
                            runner.putArgument("pkg_fusionstorbagent", FusionstorGlobalProperty.BACKUP_STORAGE_PACKAGE_NAME);
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
                                    FusionstorGlobalProperty.BACKUP_STORAGE_AGENT_PORT, ECHO_PATH), new Completion(trigger) {
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
                        self.setStatus(MonStatus.Connected);
                        dbf.update(self);
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        self.setStatus(MonStatus.Disconnected);
                        dbf.update(self);
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }
}
