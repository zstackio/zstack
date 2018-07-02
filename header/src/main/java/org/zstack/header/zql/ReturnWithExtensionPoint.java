package org.zstack.header.zql;

import java.util.List;
import java.util.Map;

public interface ReturnWithExtensionPoint {
    String getReturnWithName();

    void returnWith(List vos, String expr, Map result);
}
