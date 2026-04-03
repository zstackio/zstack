package org.zstack.header.core.workflow;

import org.zstack.utils.DebugUtils;
import org.zstack.utils.FieldUtils;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface Flow {
    void run(FlowTrigger trigger, Map data);

    void rollback(FlowRollback trigger, Map data);

    default boolean skip(Map data) {
        return false;
    }

    default String name() {
        String innerName = FieldUtils.getFieldValue("__name__", this);
        if (innerName != null && !innerName.trim().isEmpty()) {
            return innerName;
        }
        return String.format("%s", this.getClass().getSimpleName());
    }

    @SuppressWarnings("rawtypes")
    class FlowBuilder {
        private final String flowName;
        private Predicate<Map> skipPredicate;
        private BiConsumer<FlowTrigger, Map> triggerConsumer;
        private BiConsumer<FlowRollback, Map> rollbackConsumer;

        private FlowBuilder(String flowName) {
            DebugUtils.Assert(flowName != null, "flowName should not be null");
            this.flowName = flowName;
        }

        public FlowBuilder skipIf(Predicate<Map> predicate) {
            DebugUtils.Assert(predicate != null, "skipPredicate should not be null");
            this.skipPredicate = predicate;
            return this;
        }

        public FlowBuilder runIf(Predicate<Map> predicate) {
            DebugUtils.Assert(predicate != null, "runIf predicate should not be null");
            this.skipPredicate = predicate.negate();
            return this;
        }

        public FlowBuilder handle(BiConsumer<FlowTrigger, Map> consumer) {
            DebugUtils.Assert(consumer != null, "handle consumer should not be null");
            this.triggerConsumer = consumer;
            return this;
        }

        public FlowBuilder handle(Consumer<FlowTrigger> consumer) {
            DebugUtils.Assert(consumer != null, "handle consumer should not be null");
            this.triggerConsumer = (trigger, data) -> consumer.accept(trigger);
            return this;
        }

        public FlowBuilder rollback(BiConsumer<FlowRollback, Map> consumer) {
            DebugUtils.Assert(consumer != null, "rollback consumer should not be null");
            this.rollbackConsumer = consumer;
            return this;
        }

        public FlowBuilder rollback(Consumer<FlowRollback> consumer) {
            DebugUtils.Assert(consumer != null, "rollback consumer should not be null");
            this.rollbackConsumer = (trigger, data) -> consumer.accept(trigger);
            return this;
        }

        public Flow build() {
            DebugUtils.Assert(triggerConsumer != null, "handle() must be called before build()");
            Predicate<Map> finalSkipPredicate = skipPredicate;
            BiConsumer<FlowTrigger, Map> finalTriggerConsumer = triggerConsumer;
            BiConsumer<FlowRollback, Map> finalRollbackConsumer = rollbackConsumer;

            return new Flow() {
                @Override
                public void run(FlowTrigger trigger, Map data) {
                    finalTriggerConsumer.accept(trigger, data);
                }

                @Override
                public void rollback(FlowRollback trigger, Map data) {
                    if (finalRollbackConsumer == null) {
                        trigger.rollback();
                    } else {
                        finalRollbackConsumer.accept(trigger, data);
                    }
                }

                @Override
                public boolean skip(Map data) {
                    return finalSkipPredicate != null && finalSkipPredicate.test(data);
                }

                @Override
                public String name() {
                    return flowName;
                }

                @Override
                public String toString() {
                    return name();
                }
            };
        }
    }

    static FlowBuilder of(String flowName) {
        return new FlowBuilder(flowName);
    }
}
