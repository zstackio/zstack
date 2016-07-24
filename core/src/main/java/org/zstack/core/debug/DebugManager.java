package org.zstack.core.debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by xing5 on 2016/7/25.
 */
public interface DebugManager {
    Map<DebugSignal, List<DebugSignalHandler>> sigHandlers = new HashMap<>();

    static void registerDebugSignalHandler(DebugSignal sig, DebugSignalHandler handler) {
        List<DebugSignalHandler> hs = sigHandlers.get(sig);
        if (hs == null) {
            hs = new ArrayList<>();
            sigHandlers.put(sig, hs);
        }
        hs.add(handler);
    }
}
