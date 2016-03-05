package org.zstack.query;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.QueryCondition;

import java.util.List;

public interface QueryFacade {
    <T> List<T> query(APIQueryMessage msg, Class<T> inventoryClass);

    long count(APIQueryMessage msg, Class inventoryClass);
}
