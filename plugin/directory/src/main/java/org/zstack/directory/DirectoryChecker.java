package org.zstack.directory;

import org.zstack.header.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author shenjin
 * @date 2022/12/10 23:43
 */
public interface DirectoryChecker {

    boolean check(String directoryUuid, List<String> resourceUuids);

    DirectoryType getType();
}
