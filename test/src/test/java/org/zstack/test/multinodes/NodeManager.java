package org.zstack.test.multinodes;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.ExceptionDSL.throwableSafe;
import static org.zstack.utils.StringDSL.s;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class NodeManager {
    private static final CLogger logger = Utils.getLogger(NodeManager.class);
    private List<NodeRunner> nodes = new ArrayList<NodeRunner>();
    private String properties;
    private int portStart = 8100;
    private int nodeStart = 0;

    private String properties(int port) {
        return s(
                "-DRESTFacade.port={port} ",
                "-DKVMHostFactory.agentPort={port} ",
                "-DSftpBackupStorageFactory.agentPort={port} ",
                "-DVirtualRouterManager.agentPort={port} ",
                "-DManagementServerConsoleProxyBackend.agentPort={port} "
        ).formatByMap(map(e("port", port)));
    }

    public NodeManager setProperties(String prop) {
        properties = prop;
        return this;
    }

    public List<NodeRunner> getNodes() {
        return nodes;
    }

    public void stopNode(final int index, int timeout) {
        NodeRunner node = CollectionUtils.find(nodes, new Function<NodeRunner, NodeRunner>() {
            @Override
            public NodeRunner call(NodeRunner arg) {
                if (arg.getUuid() == index) {
                    return arg;
                }

                return null;
            }
        });

        stopNode(node, timeout);
    }

    public void stopNode(final String managementNodeId, int timeout) {
        NodeRunner node = CollectionUtils.find(nodes, new Function<NodeRunner, NodeRunner>() {
            @Override
            public NodeRunner call(NodeRunner arg) {
                if (arg.getManagementNodeId().equals(managementNodeId)) {
                    return arg;
                }
                return null;
            }
        });

        stopNode(node, timeout);
    }

    public void asyncStopNode(final int index, final int timeout) {
        new Runnable() {
            @Override
            @AsyncThread
            public void run() {
                stopNode(index, timeout);
            }
        }.run();
    }

    public void createLightWeightNodeWithCore(int num, final int timeout, List<String> springConfigs) {
        List<NodeRunner> ns = new ArrayList<NodeRunner>();

        for (int i = 0; i < num; i++) {
            NodeRunner node = new NodeRunner();
            int port = 8100 + i;
            node.setPort(port);
            node.setSpringConfigs(springConfigs);
            node.setServiceId("node" + i);
            node.setUuid(i);
            List<String> sconfs = new ArrayList<String>();
            sconfs.addAll(springConfigs);
            sconfs.addAll(Arrays.asList("PortalForUnitTest.xml", "AccountManager.xml"));
            node.setSpringConfigs(sconfs);
            if (properties != null) {
                node.setPropertyString(properties);
            }
            node.run();
            ns.add(node);
        }

        final CountDownLatch latch = new CountDownLatch(num);
        final List<String> lst = new ArrayList<String>();
        for (final NodeRunner node : ns) {
            new Runnable() {
                @Override
                @AsyncThread
                public void run() {
                    throwableSafe(new Runnable() {
                        @Override
                        public void run() {
                            if (!node.waitNodeStart(timeout)) {
                                lst.add(String.format("node[%s] start timeout after %s seconds", node.getServiceId(), timeout));
                            }
                        }
                    });

                    latch.countDown();
                }
            }.run();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new CloudRuntimeException(e);
        }

        if (!lst.isEmpty()) {
            throw new CloudRuntimeException(StringUtils.join(lst, "\n"));
        }

        nodes.addAll(ns);

    }

    public void createNodes(int num, final int timeout) {
        List<NodeRunner> ns = new ArrayList<NodeRunner>();

        for (int i = 0; i < num; i++) {
            NodeRunner node = new NodeRunner();
            int port = portStart++;
            node.setLoadAll(true);
            node.setPort(port);
            node.setServiceId("node" + nodeStart++);
            node.setUuid(i);
            if (properties != null) {
                properties = properties(port) + properties;
            } else {
                properties = properties(port);
            }
            node.setPropertyString(properties);
            node.run();
            ns.add(node);
        }

        final CountDownLatch latch = new CountDownLatch(num);
        final List<String> lst = new ArrayList<String>();
        for (final NodeRunner node : ns) {
            new Runnable() {
                @Override
                @AsyncThread
                public void run() {
                    throwableSafe(new Runnable() {
                        @Override
                        public void run() {
                            if (!node.waitNodeStart(timeout)) {
                                lst.add(String.format("node[%s] start timeout after %s seconds", node.getServiceId(), timeout));
                            }
                        }
                    });

                    latch.countDown();
                }
            }.run();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new CloudRuntimeException(e);
        }

        if (!lst.isEmpty()) {
            throw new CloudRuntimeException(StringUtils.join(lst, "\n"));
        }

        nodes.addAll(ns);
    }

    private void stopNode(final NodeRunner node, final int timeout) {
        throwableSafe(new Runnable() {
            @Override
            public void run() {
                if (!node.waitNodeExit(timeout)) {
                    logger.debug(String.format("failed to stop node[%s] in %s seconds, kill it", node.getServiceId(), timeout));
                    node.kill();
                }
            }
        });
    }

    public void stopNodes(final int timeout) {
        final CountDownLatch latch = new CountDownLatch(nodes.size());
        for (final NodeRunner node : nodes) {
            new Runnable() {
                @Override
                @AsyncThread
                public void run() {
                    stopNode(node, timeout);
                    latch.countDown();
                }
            }.run();
        }

        try {
            latch.await(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new CloudRuntimeException(e);
        }
    }
}
