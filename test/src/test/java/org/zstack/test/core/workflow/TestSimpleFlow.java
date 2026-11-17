package org.zstack.test.core.workflow;

import junit.framework.Assert;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.Test;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.core.workflow.WorkFlowException;
import org.zstack.core.workflow.WorkFlowStatistic;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 3:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestSimpleFlow {
    CLogger logger = Utils.getLogger(TestSimpleFlow.class);
    boolean success;

    @Test
    public void test() throws WorkFlowException {
        final int[] count = {0};

        new SimpleFlowChain()
                .then(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger chain, Map data) {
                        count[0]++;
                        chain.next();
                    }
                })
                .then(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger chain, Map data) {
                        success = count[0] == 1;
                    }
                })
                .start();

        Assert.assertTrue(success);
    }

    @Test
    public void testWatchLogsCurrentExecutionTime() {
        String flowName = "duration-log-" + UUID.randomUUID();
        String tailName = flowName + "-tail";
        WorkFlowStatistic statistic = new WorkFlowStatistic();
        statistic.setName(flowName);
        statistic.addStatistic(60000);
        SimpleFlowChain.getStatistics().put(flowName, statistic);

        List<String> messages = new ArrayList<>();
        AbstractAppender appender = new AbstractAppender(flowName, null, null, false, Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                String message = event.getMessage().getFormattedMessage();
                if (message.contains(", flow:" + flowName + "] takes ")) {
                    messages.add(message);
                }
            }
        };
        Logger flowLogger = (Logger) LogManager.getLogger(SimpleFlowChain.class);
        Level oldLevel = flowLogger.getLevel();
        boolean oldAdditivity = flowLogger.isAdditive();
        appender.start();
        flowLogger.setAdditive(false);
        flowLogger.addAppender(appender);
        flowLogger.setLevel(Level.DEBUG);
        try {
            for (int run = 0; run < 2; run++) {
                long previousTotal = statistic.getTotalTime();
                SimpleFlowChain chain = new SimpleFlowChain();
                chain.allowWatch();
                chain.then(new NoRollbackFlow() {
                    final String __name__ = flowName;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        trigger.next();
                    }
                }).then(new NoRollbackFlow() {
                    final String __name__ = tailName;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        trigger.next();
                    }
                }).start();

                Assert.assertEquals(run + 1, messages.size());
                long duration = statistic.getTotalTime() - previousTotal;
                Assert.assertTrue(messages.get(run), messages.get(run).endsWith(
                        " takes " + duration + "ms to complete"));
                Assert.assertEquals(run + 2, statistic.getCount());
                Assert.assertEquals(statistic.getTotalTime() / statistic.getCount(), statistic.getAverageTime());
            }
        } finally {
            flowLogger.removeAppender(appender);
            flowLogger.setAdditive(oldAdditivity);
            flowLogger.setLevel(oldLevel);
            appender.stop();
            SimpleFlowChain.getStatistics().remove(flowName);
            SimpleFlowChain.getStatistics().remove(tailName);
        }
    }
}
