package org.zstack.core.agent;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.Message;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by frank on 12/5/2015.
 */
public class AgentManagerImpl extends AbstractService implements AgentManager {
    @Autowired
    private CloudBus bus;
    @Autowired
    private AnsibleFacade asf;
    @Autowired
    private ThreadFacade thdf;

    private Map<String, Map<String, AgentStruct>> agents = new HashMap<String, Map<String, AgentStruct>>();

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof DeployAgentMsg) {
            handle((DeployAgentMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
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

    private void deployAgent(DeployAgentMsg msg, NoErrorCompletion noErrorCompletion) {
        DeployAgentReply reply = new DeployAgentReply();
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(AgentConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        asf.deployModule(AgentConstant.ANSIBLE_MODULE_PATH, AgentConstant.ANSIBLE_PLAYBOOK_NAME);
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
    }
}
