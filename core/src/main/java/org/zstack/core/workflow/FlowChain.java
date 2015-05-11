package org.zstack.core.workflow;

import java.util.List;
import java.util.Map;

/**
 */
public interface FlowChain {
    List<Flow> getFlows();

    FlowChain insert(Flow flow);

    FlowChain insert(int pos, Flow flow);

    FlowChain then(Flow flow);

    FlowChain done(FlowDoneHandler handler);

    FlowChain error(FlowErrorHandler handler);

    FlowChain setData(Map data);

    FlowChain setName(String name);

    Map getData();

    void start();

    FlowChain noRollback(boolean no);

    FlowChain allowEmptyFlow();
}
