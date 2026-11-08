package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.vm.RestoreVmExtensionPoint;
import org.zstack.header.vm.RestoreVmMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.zstack.header.vm.RestoreVmExtensionPoint.ROLLBACK_ERROR;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmPreRestoreFlow implements Flow {
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        List<Flow> started = new ArrayList<>();
        data.put(VmPreRestoreFlow.class.getName(), started);
        RestoreVmMsg msg = (RestoreVmMsg) data.get(RestoreVmMsg.class.getName());
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        List<Flow> flows = new ArrayList<>();
        for (RestoreVmExtensionPoint ext : pluginRgty.getExtensionList(RestoreVmExtensionPoint.class)) {
            Flow flow = ext.getPreRestoreVmFlow(msg, spec);
            if (flow != null) {
                flows.add(flow);
            }
        }
        runExtensions(trigger, data, flows, started);
    }

    static void runExtensions(FlowTrigger trigger, Map data, List<Flow> flows, List<Flow> started) {
        new While<>(flows).each((flow, compl) -> runExtension(flow, data, started, trigger, new Completion(compl) {
            @Override
            public void success() {
                compl.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                compl.addError(errorCode);
                compl.allDone();
            }
        })).run(new WhileDoneCompletion(trigger) {
            @Override
            public void done(ErrorCodeList errors) {
                if (errors.getCauses().isEmpty()) {
                    trigger.next();
                } else {
                    trigger.fail(errors.getCauses().get(0));
                }
            }
        });
    }

    private static void runExtension(Flow flow, Map data, List<Flow> started, FlowTrigger trigger, Completion completion) {
        if (flow.skip(data)) {
            completion.success();
            return;
        }
        started.add(flow);
        flow.run(new FlowTrigger() {
            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }

            @Override
            public void next() {
                completion.success();
            }

            @Override
            public void setError(ErrorCode error) {
                trigger.setError(error);
            }
        }, data);
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        if (data.containsKey(ROLLBACK_ERROR)) {
            trigger.skipRestRollbacks();
            trigger.rollback();
            return;
        }
        rollbackExtensions(trigger, data, VmPreRestoreFlow.class.getName());
    }

    static void rollbackExtensions(FlowRollback trigger, Map data, String key) {
        List<Flow> started = (List<Flow>) data.get(key);
        List<Flow> flows = new ArrayList<>(started);
        started.clear();
        Collections.reverse(flows);
        new While<>(flows).each((flow, compl) -> {
            class ExtensionRollback extends Completion implements FlowRollback {
                private boolean skip;

                ExtensionRollback() {
                    super(compl);
                }

                @Override
                public void success() {
                    if (skip || data.containsKey(ROLLBACK_ERROR)) {
                        compl.allDone();
                    } else {
                        compl.done();
                    }
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    compl.addError(errorCode);
                    compl.allDone();
                }

                @Override
                public void rollback() {
                    success();
                }

                @Override
                public void skipRestRollbacks() {
                    skip = true;
                    trigger.skipRestRollbacks();
                }

                @Override
                public ErrorCode getErrorCode() {
                    return trigger.getErrorCode();
                }
            }
            flow.rollback(new ExtensionRollback(), data);
        }).run(new WhileDoneCompletion(trigger) {
            @Override
            public void done(ErrorCodeList errors) {
                if (!errors.getCauses().isEmpty()) {
                    data.put(ROLLBACK_ERROR, errors.getCauses().get(0));
                    VmInstanceBase.logger.warn(String.format("failed to roll back VM restore extensions, %s", errors.getCauses()));
                }
                if (data.containsKey(ROLLBACK_ERROR)) {
                    trigger.skipRestRollbacks();
                }
                trigger.rollback();
            }
        });
    }
}
