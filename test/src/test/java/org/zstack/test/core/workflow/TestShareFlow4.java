package org.zstack.test.core.workflow;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.test.BeanConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 */
public class TestShareFlow4 {
    boolean success;

    @Test
    public void test() {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();

        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    @AfterDone
                    List<Runnable> afterDone = new ArrayList<Runnable>();

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        afterDone.add(() -> {
                            success = true;
                        });

                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(null) {
            @Override
            public void handle(Map data) {
            }
        }).start();
        Assert.assertTrue(success);

        success = false;
        chain = FlowChainBuilder.newShareFlowChain();
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    @AfterError
                    List<Runnable> afterError = new ArrayList<Runnable>();

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        afterError.add(() -> {
                            success = true;
                        });

                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        trigger.fail(null);
                    }
                });
            }
        }).done(new FlowDoneHandler(null) {
            @Override
            public void handle(Map data) {
            }
        }).error(new FlowErrorHandler(null) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
            }
        }).start();
        Assert.assertTrue(success);

        success = false;
        chain = FlowChainBuilder.newShareFlowChain();
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    @AfterFinal
                    List<Runnable> afterFinal = new ArrayList<Runnable>();

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        afterFinal.add(() -> {
                            success = true;
                        });

                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        trigger.fail(null);
                    }
                });
            }
        }).done(new FlowDoneHandler(null) {
            @Override
            public void handle(Map data) {
            }
        }).error(new FlowErrorHandler(null) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
            }
        }).start();
        Assert.assertTrue(success);
    }


    @Before
    public void setUp() throws Exception {
        new BeanConstructor().build();
    }
}
