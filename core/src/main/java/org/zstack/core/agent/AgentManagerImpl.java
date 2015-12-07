package org.zstack.core.agent;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.ansible.AnsibleRunner;
import org.zstack.core.ansible.SshFolderMd5Checker;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.Message;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.ssh.Ssh;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.StringDSL.ln;

/**
 * Created by frank on 12/5/2015.
 */
public class AgentManagerImpl extends AbstractService implements AgentManager {
    private static final CLogger logger = Utils.getLogger(AgentManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;

    private Map<String, Map<String, AgentStruct>> agents = new HashMap<String, Map<String, AgentStruct>>();
    private String srcRootFolder;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof DeployAgentMsg) {
            handle((DeployAgentMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    void init() {
        ShellUtils.run("mkdir -p %s", AgentConstant.SRC_ANSIBLE_ROOT);
        File srcFolder = PathUtil.findFolderOnClassPath(AgentConstant.ANSIBLE_MODULE_PATH, true);
        srcRootFolder = srcFolder.getAbsolutePath();
        ShellUtils.run(String.format("yes | cp -r %s/server %s", srcRootFolder, AgentConstant.SRC_ANSIBLE_ROOT));
    }

    private void handle(final DeployAgentMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("deploy-agent-to-server-%s", msg.getIp());
            }

            @Override
            public void run(final SyncTaskChain chain) {
                deployAgent(msg, new NoErrorCompletion(msg, chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void deployAgent(final DeployAgentMsg msg, final NoErrorCompletion noErrorCompletion) {
        final DeployAgentReply reply = new DeployAgentReply();
        Map<String, AgentStruct> m = agents.get(msg.getOwner());
        if (m == null) {
            logger.warn(String.format("no plugins found for the agent[owner:%s], skip deploying agent", msg.getOwner()));
            bus.reply(msg, reply);
            noErrorCompletion.done();
            return;
        }

        try {
            String agentYamlPath = PathUtil.join(AgentConstant.ANSIBLE_MODULE_PATH, AgentConstant.ANSIBLE_PLAYBOOK_NAME);
            File agentYaml = PathUtil.findFileOnClassPath(agentYamlPath);
            if (agentYaml == null) {
                throw new CloudRuntimeException(String.format("cannot find agent yaml file[%s] on the class path", agentYamlPath));
            }

            final File include = File.createTempFile("zstack", "ansibleInclude");
            StringBuilder sb = new StringBuilder("---\n\n");
            for (AgentStruct s : m.values()) {
                sb.append(String.format("- include: %s\n", s.getAnsibleYaml()));
            }
            FileUtils.writeStringToFile(include, sb.toString());

            String agentYamlContent = FileUtils.readFileToString(agentYaml);
            agentYamlContent = ln(agentYamlContent).formatByMap(map(
                    e("root", AgentConstant.DST_ANSIBLE_ROOT),
                    e("agentYamls", String.format("%s", include.getAbsolutePath()))
            ));

            final File tmpAgentYaml = File.createTempFile("zstack", "ansilbeTempAgent");
            FileUtils.writeStringToFile(tmpAgentYaml, agentYamlContent);

            SshFolderMd5Checker checker = new SshFolderMd5Checker();
            checker.setPassword(msg.getPassword());
            checker.setUsername(msg.getUsername());
            if (msg.getSshPort() != null) {
                checker.setPort(msg.getSshPort());
            }
            checker.setHostname(msg.getIp());
            checker.setSrcFolder(srcRootFolder);
            checker.setDstFolder(AgentConstant.DST_ANSIBLE_ROOT);
            if (checker.needDeploy()) {
                Ssh ssh = new Ssh();
                ssh.setPassword(msg.getPassword());
                ssh.setUsername(msg.getUsername());
                ssh.setHostname(msg.getIp());
                if (msg.getSshPort() != null) {
                    ssh.setPort(msg.getSshPort());
                }
                ssh.command(String.format("mkdir -p %s", AgentConstant.DST_ANSIBLE_ROOT))
                        .scp(srcRootFolder, AgentConstant.DST_ANSIBLE_ROOT);
                ssh.runErrorByExceptionAndClose();
            }

            AnsibleRunner runner = new AnsibleRunner();
            runner.setPassword(msg.getPassword());
            runner.setUsername(msg.getUsername());
            runner.setAgentPort(AgentConstant.AGENT_PORT);
            if (msg.getSshPort() != null) {
                runner.setSshPort(msg.getSshPort());
            }
            runner.setTargetIp(msg.getIp());
            runner.setPlayBookName(tmpAgentYaml.getAbsolutePath());
            runner.run(new Completion(msg, noErrorCompletion) {
                @Override
                @Deferred
                public void success() {
                    Defer.defer(new Runnable() {
                        @Override
                        public void run() {
                            include.delete();
                            tmpAgentYaml.delete();
                        }
                    });

                    bus.reply(msg, reply);
                    noErrorCompletion.done();
                }

                @Override
                @Deferred
                public void fail(ErrorCode errorCode) {
                    Defer.defer(new Runnable() {
                        @Override
                        public void run() {
                            include.delete();
                            tmpAgentYaml.delete();
                        }
                    });

                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                    noErrorCompletion.done();
                }
            });
        } catch (IOException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(AgentConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void registerAgent(AgentStruct struct) {
        Map<String, AgentStruct> m = agents.get(struct.getAgentOwner());
        if (m == null) {
            m = new HashMap<String, AgentStruct>();
            agents.put(struct.getAgentOwner(), m);
        }

        AgentStruct old = m.get(struct.getAgentId());
        if (old != null) {
            throw new CloudRuntimeException(String.format("there has been an agent[id:%s] registered to the owner[%s]", struct.getAgentId(), struct.getAgentOwner()));
        }

        m.put(struct.getAgentId(), struct);
        ShellUtils.run(String.format("yes | cp -r %s %s", struct.getFileFolder(), AgentConstant.SRC_ANSIBLE_ROOT));
    }
}
