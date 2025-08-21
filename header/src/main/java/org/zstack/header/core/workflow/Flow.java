package org.zstack.header.core.workflow;

import org.zstack.utils.FieldUtils;

import java.util.Map;

public interface Flow {
    void run(FlowTrigger trigger, Map data);

    void rollback(FlowRollback trigger, Map data);

    default boolean skip(Map data) {
        return false;
    }

    /**
     * @since zsv 4.10.20
     */
    default String name() {
        String innerName = FieldUtils.getFieldValue("__name__", this);
        if (innerName != null && !innerName.trim().isEmpty()) {
            return innerName;
        }
        return String.format("%s", this.getClass().getSimpleName());
    }
}
