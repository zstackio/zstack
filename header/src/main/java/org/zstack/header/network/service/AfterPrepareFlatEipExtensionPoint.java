package org.zstack.header.network.service;

import org.zstack.header.core.Completion;

import java.util.List;

public interface AfterPrepareFlatEipExtensionPoint {
    void afterPrepareFlatEip(List<String> vipUuids, String hostUuid, Completion completion);
}
