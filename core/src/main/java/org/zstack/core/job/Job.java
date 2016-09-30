package org.zstack.core.job;

import org.zstack.header.core.ReturnValueCompletion;

import java.io.Serializable;

public interface Job extends Serializable {
    void run(ReturnValueCompletion<Object> completion);
}
