package org.zstack.header.zql;

import java.util.List;
import java.util.Map;

public interface ReturnWithExtensionPoint {
    class ReturnWithExtensionParam {
        public List vos;
        public String expression;
        public boolean isFieldsQuery;
        public Integer primaryKeyIndexInVOs;
        public Class voClass;
    }

    String getReturnWithName();

    Map returnWith(ReturnWithExtensionParam param);
}
