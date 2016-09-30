package org.zstack.test.core.errorcode;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestErrorCode {
    CLogger logger = Utils.getLogger(TestErrorCode.class);
    ComponentLoader loader;
    ErrorFacade errf;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        errf = loader.getComponent(ErrorFacade.class);
    }

    @Test
    public void test() {
        ErrorCode err = errf.stringToInternalError("on purpose");
        logger.debug(err.toString());
        err = errf.stringToTimeoutError("on purpose");
        logger.debug(err.toString());
    }
}
