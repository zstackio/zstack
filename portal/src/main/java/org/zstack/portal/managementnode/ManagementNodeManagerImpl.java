package org.zstack.portal.managementnode;

import com.rabbitmq.client.AlreadyClosedException;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.GLock;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.*;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.Service;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.managementnode.IsManagementNodeReadyMsg;
import org.zstack.header.managementnode.IsManagementNodeReadyReply;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.*;
import org.zstack.header.managementnode.ManagementNodeExitMsg.Reason;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.portal.apimediator.ApiMediator;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.ExceptionDSL.throwableSafe;
import static org.zstack.utils.ExceptionDSL.throwableSafeSuppress;

public class ManagementNodeManagerImpl extends AbstractService implements ManagementNodeManager, ManagementNodeChangeListener {
	private static final CLogger logger = Utils.getLogger(ManagementNodeManager.class);
	private ManagementNode node;

	private boolean forceInventory = false;
	private List<ComponentWrapper> components;
	private List<PrepareDbInitialValueExtensionPoint> prepareDbExts;
	private ComponentLoader loader;
	private volatile boolean isRunning = true;
	private volatile int isNodeRunning = NODE_STARTING;
	private static final String INVENTORY_LOCK = "ManagementNodeManager.inventory_lock";
	private final int INVENTORY_LOCK_TIMEOUT = 600; /* 10 mins */
	private static boolean started = false;
	private static boolean stopped = false;
    private static boolean isDbDead = false;
	
	private static int NODE_STARTING = 0;
	private static int NODE_RUNNING = 1;
	private static int NODE_FAILED = -1;

	@Autowired
	private DatabaseFacade dbf;
	@Autowired
	private CloudBusIN bus;
	@Autowired
	private ApiMediator apim;
	@Autowired
	private PluginRegistry pluginRgty;
    @Autowired
    private ThreadFacade thdf;

    private List<ManagementNodeChangeListener> lifeCycleExtension = new ArrayList<ManagementNodeChangeListener>();

    private interface ComponentWrapper {
        void start();
        void stop();
    }

	private void notifyStop() {
		isRunning = false;
		synchronized (this) {
			this.notify();
		}
	}
	
	private void handle(ManagementNodeExitMsg msg) {
		logger.debug(getId() + " received ManagementNodeExitMsg, going to exit");
        if (msg.getReason() == Reason.HeartBeatStopped) {
            isDbDead = true;
        }

		notifyStop();
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

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIListManagementNodeMsg) {
            handle((APIListManagementNodeMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIListManagementNodeMsg msg) {
        List<ManagementNodeVO> vos = dbf.listAll(ManagementNodeVO.class);
        APIListManagementNodeReply reply = new APIListManagementNodeReply();
        reply.setInventories(ManagementNodeInventory.valueOf(vos));
        bus.reply(msg, reply);
    }

    private void handleLocalMessage(Message msg) {
        if (msg.getClass() == ManagementNodeExitMsg.class) {
			handle((ManagementNodeExitMsg) msg);
        } else if (msg instanceof IsManagementNodeReadyMsg) {
            handle((IsManagementNodeReadyMsg)msg);
		} else {
			bus.dealWithUnknownMessage(msg);
		}
    }

    private void handle(IsManagementNodeReadyMsg msg) {
        IsManagementNodeReadyReply reply = new IsManagementNodeReadyReply();
        reply.setReady(isNodeRunning == NODE_RUNNING);
        bus.reply(msg, reply);
    }

	@Override
	public String getId() {
		return bus.makeLocalServiceId(ManagementNodeConstant.SERVICE_ID);
	}

	private void saveInventory() {
		ManagementNodeContextVO vo;
		vo = dbf.findById(1, ManagementNodeContextVO.class);
		if (vo == null) {
			vo = new ManagementNodeContextVO();
		}
		ManagementNodeContextInventory inv = new ManagementNodeContextInventory();
		inv.setVersion(Platform.getCodeVersion());
		vo.setInventory(inv.toBytes());
		dbf.updateAndRefresh(vo);
	}

	private void compareInventory() {
		ManagementNodeContextVO vo;
		vo = dbf.findById(1, ManagementNodeContextVO.class);
		if (vo == null) {
			saveInventory();
			return;
		}

		ManagementNodeContextInventory inv = ManagementNodeContextInventory.fromBytes(vo.getInventory());
		if (!inv.getVersion().equals(Platform.getCodeVersion())) {
			String details = "Expected version is " + inv.getVersion() + " but our version is " + Platform.getCodeVersion();
			throw new CloudRuntimeException(details);
		}
	}

	private void removeInventory(boolean needLock) {
        if (needLock) {
            GLock lock = new GLock(INVENTORY_LOCK, INVENTORY_LOCK_TIMEOUT);
            lock.lock();
            try {
                if (node != null && node.getNodes().size() == 1) {
                    dbf.removeByPrimaryKey(1L, ManagementNodeContextVO.class);
                }
            } finally {
                lock.unlock();
            }
        } else {
            if (node != null && node.getNodes().size() == 1) {
                dbf.removeByPrimaryKey(1L, ManagementNodeContextVO.class);
            }
        }

	}

	private void checkInventory() {
		if (forceInventory) {
			saveInventory();
		} else {
			compareInventory();
		}
	}

	private void startComponents() {
		for (ComponentWrapper c : components) {
            c.start();
		}
	}

	private void stopComponents() {
        for (final ComponentWrapper c : components) {
            c.stop();
        }
	}

	private void installShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
                logger.debug("JVM shutdown hook is called, start stopping management node");
				stop();
			}
		}));
	}

	private void populateComponents() {
		components = new ArrayList<ComponentWrapper>();
		for (final Component c : pluginRgty.getExtensionList(Component.class)) {
			components.add(new ComponentWrapper() {
                boolean isStart = false;

                @Override
                public void start() {
                    c.start();
                    logger.info("Started component: " + c.getClass().getName());
                    isStart = true;
                }

                @Override
                public void stop() {
                    if (isStart) {
                        throwableSafe(new Runnable() {
                            @Override
                            public void run() {
                                c.stop();
                                logger.info("Stopped component: " + c.getClass().getName());
                                isStart = false;
                            }
                        }, String.format("unable to stop component[%s]", c.getClass().getName()));
                    }
                }
            });
		}

		prepareDbExts = pluginRgty.getExtensionList(PrepareDbInitialValueExtensionPoint.class);
	}

	private void callPrepareDbExtensions() {
	    for (PrepareDbInitialValueExtensionPoint extp : prepareDbExts) {
	        extp.prepareDbInitialValue();
	    }
	}

    private void populateExtensions() {
        lifeCycleExtension = pluginRgty.getExtensionList(ManagementNodeChangeListener.class);
    }

	@Override
	public boolean start() {
	    if (started) {
	        /* largely for unittest, the ComponentLoaderWebListener and Api may both call start()
	         */
	        logger.debug(String.format("Management Node has already started, ignore this call"));
	        return true;
	    }

        populateExtensions();
	    
	    started = true;
        stopped = true;

        class Result {
            boolean success;
        }

        final Result ret = new Result();

        GLock lock = new GLock(INVENTORY_LOCK, INVENTORY_LOCK_TIMEOUT);
		/*
	     * The lock is being held until we join in, otherwise the inventory
	     * may be deleted by other exiting node because we have not
		 * persisted our entry in management_node table yet, or two starting
		 * nodes persist inventory concurrently.
	     */
        lock.lock();
		try {
            final ManagementNodeManagerImpl self = this;
            FlowChain bootstrap = FlowChainBuilder.newSimpleFlowChain();
            bootstrap.setName("management-node-bootstrap");
            bootstrap.then(new Flow() {
                String __name__ = "bootstrap-cloudbus";

                // CloudBus is special, it initializes in Platform.createComponentLoaderFromWebApplicationContext(),
                // however, when exception happens in bootstrap we need to stop bus in rollback, because the exception
                // cannot make JVM exist and cloudbus.stop is only called in JVM exit hook;
                @Override
                public void run(FlowTrigger trigger, Map data) {
                    trigger.next();
                }

                @Override
                public void rollback(FlowRollback trigger, Map data) {
                    bus.stop();
                    trigger.rollback();
                }
            }).then(new NoRollbackFlow() {
                @Override
                public void run(FlowTrigger trigger, Map data) {
                    loader = Platform.getComponentLoader();
                    populateComponents();
                    trigger.next();
                }
            }).then(new Flow() {
                String __name__ = "register-node-on-cloudbus";
                @Override
                public void run(FlowTrigger trigger, Map data) {
                    bus.registerService(self);
                    trigger.next();
                }

                @Override
                public void rollback(FlowRollback trigger, Map data) {
                    bus.unregisterService(self);
                    trigger.rollback();
                }
            }).then(new Flow() {
                String __name__ = "start-components";
                @Override
                public void run(FlowTrigger trigger, Map data) {
                    startComponents();
                    trigger.next();
                }

                @Override
                public void rollback(FlowRollback trigger, Map data) {
                    stopComponents();
                    trigger.rollback();
                }
            }).then(new Flow() {
                String __name__ = "check-management-node-inventory";
                @Override
                public void run(FlowTrigger trigger, Map data) {
                    checkInventory();
                    trigger.next();
                }

                @Override
                public void rollback(FlowRollback trigger, Map data) {
                    removeInventory(false);
                    trigger.rollback();
                }
            }).then(new NoRollbackFlow() {
                String __name__ = "call-prepare-db-extension";
                @Override
                public void run(FlowTrigger trigger, Map data) {
                    callPrepareDbExtensions();
                    trigger.next();
                }
            }).then(new Flow() {
                String __name__ = "join-management-node";
                @Override
                public void run(FlowTrigger trigger, Map data) {
                    node = new ManagementNode();
                    node.addNodeManagerCallback(self);
                    node.join();
                    trigger.next();
                }

                @Override
                public void rollback(FlowRollback trigger, Map data) {
                    node.leave();
                    trigger.rollback();
                }
            }).then(new Flow() {
                String __name__ = "start-api-mediator";
                @Override
                public void run(FlowTrigger trigger, Map data) {
                    apim.start();
                    trigger.next();
                }

                @Override
                public void rollback(FlowRollback trigger, Map data) {
                    apim.stop();
                    trigger.rollback();
                }
            }).done(new FlowDoneHandler() {
                @Override
                public void handle(Map data) {
                    ret.success = true;
                }
            }).error(new FlowErrorHandler() {
                @Override
                public void handle(ErrorCode errCode, Map data) {
                    String bootErrorPath = PathUtil.join(PathUtil.getZStackHomeFolder(), "bootError.log");
                    try {
                        FileUtils.writeStringToFile(new File(bootErrorPath), errCode.toString());
                    } catch (IOException e) {
                        logger.warn(String.format("unable to write error to %s", bootErrorPath));
                    }
                    ret.success = false;
                }
            }).start();
		} finally {
            lock.unlock();
		}

        if (!ret.success) {
            logger.warn(String.format("management node[%s] failed to start for some reason", Platform.getUuid()));
            stopped = true;

            if (CoreGlobalProperty.EXIT_JVM_ON_BOOT_FAILURE) {
                logger.debug(String.format("unable to start management node[%s], see previous exception. exitJVMOnBootFailure is set to true, exit JVM now", Platform.getManagementServerId()));
                System.exit(1);
            } else {
                throw new CloudRuntimeException(String.format("unable to start management node[%s], see previous exception", Platform.getManagementServerId()));
            }
        }

        stopped = false;

		installShutdownHook();

        for (ManagementNodeReadyExtensionPoint ext : pluginRgty.getExtensionList(ManagementNodeReadyExtensionPoint.class)) {
            ext.managementNodeReady();
        }

        logger.info("Management node: " + getId() + " starts successfully");

		synchronized (this) {
		    isNodeRunning = NODE_RUNNING;
			while (isRunning) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					logger.warn("Interrupted while daemon is running, continue ...", e);
				}
			}
		}

        logger.debug("quited mainloop, start stopping management node");
        stop();
		return true;
	}

	@Override
	public boolean stop() {
	    if (stopped) {
	        /* avoid repeated call from JVM shutdown hook, if process is exited from a former stop() call
	         */
	        return true;
	    }
	    
	    stopped = true;
        final Service self = this;
        throwableSafeSuppress(new Runnable() {
            @Override
            public void run() {
                bus.unregisterService(apim);
            }
        }, AlreadyClosedException.class).throwableSafe(new Runnable() {
            @Override
            public void run() {
                apim.stop();
            }
        }).throwableSafe(new Runnable() {
            @Override
            public void run() {
                node.leave();
            }
        }).throwableSafe(new Runnable() {
            @Override
            public void run() {
                stopComponents();
            }
        }).throwableSafe(new Runnable() {
            @Override
            public void run() {
                if (!isDbDead) {
                    removeInventory(true);
                }
            }
        }).throwableSafe(new Runnable() {
            @Override
            public void run() {
                bus.unregisterService(self);
            }
        }).throwableSafe(new Runnable() {
            @Override
            public void run() {
                bus.stop();
            }
        }).throwableSafe(new Runnable() {
            @Override
            public void run() {
                notifyStop();
            }
        }).throwableSafe(new Runnable() {
            @Override
            public void run() {
                thdf.stop();
            }
        });

        logger.info("Management node: " + getId() + " exits successfully");
        if (CoreGlobalProperty.EXIT_JVM_ON_STOP) {
            logger.info("exitJVMOnStop is set to true, exit the JVM");
            System.exit(0);
        }

        return true;
	}

	@Override
	public void nodeJoin(final String nodeId) {
        CollectionUtils.safeForEach(lifeCycleExtension, new ForEachFunction<ManagementNodeChangeListener>() {
            @Override
            public void run(ManagementNodeChangeListener arg) {
                arg.nodeJoin(nodeId);
            }
        });
	}

	@Override
	public void nodeLeft(final String nodeId) {
        CollectionUtils.safeForEach(lifeCycleExtension, new ForEachFunction<ManagementNodeChangeListener>() {
            @Override
            public void run(ManagementNodeChangeListener arg) {
                arg.nodeLeft(nodeId);
            }
        });
	}

	@Override
	public void iAmDead(final String nodeId) {
        CollectionUtils.safeForEach(lifeCycleExtension, new ForEachFunction<ManagementNodeChangeListener>() {
            @Override
            public void run(ManagementNodeChangeListener arg) {
                arg.iAmDead(nodeId);
            }
        });
	}

	@Override
	public void iJoin(final String nodeId) {
        CollectionUtils.safeForEach(lifeCycleExtension, new ForEachFunction<ManagementNodeChangeListener>() {
            @Override
            public void run(ManagementNodeChangeListener arg) {
                arg.iJoin(nodeId);
            }
        });
	}

	public void setForceInventory(boolean forceInventory) {
		this.forceInventory = forceInventory;
	}

	@AsyncThread
	private void startInThread() {
        try {
            start();
            isNodeRunning = NODE_RUNNING;
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
            isNodeRunning = NODE_FAILED;
        }
	}
	
    @Override
    public void startNode() {
        startInThread();
        while (isNodeRunning == NODE_STARTING) {
            logger.debug("management node is still initializing ...");
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new CloudRuntimeException(e);
            } 
        }
        
        if (isNodeRunning == NODE_FAILED) {
            logger.debug(String.format("error happened when starting node, stop the management node now"));
            stop();
            throw new CloudRuntimeException("failed to start management node");
        }
    }
}
