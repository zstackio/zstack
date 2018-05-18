package org.zstack.header.identity;

import org.zstack.header.vo.ResourceVO;
import org.zstack.utils.FieldUtils;

public interface OwnedByAccount {
    String getAccountUuid();
    void setAccountUuid(String accountUuid);

    static String getResourceUuid(Object entity) {
        if (entity instanceof ResourceVO) {
            return ((ResourceVO) entity).getUuid();
        }

        return FieldUtils.getFieldValue("uuid", entity);
    }
}
