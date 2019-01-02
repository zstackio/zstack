package org.zstack.test.core.errorcode;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.inerr;
import static org.zstack.core.Platform.touterr;

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
        ErrorCode err = inerr("on purpose");
        logger.debug(err.toString());
        err = touterr("on purpose");
        logger.debug(err.toString());
    }
}
