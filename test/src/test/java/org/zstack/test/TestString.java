package org.zstack.test;

import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.db.DBGraph;
import org.zstack.header.image.ImageVO;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.zone.ZoneVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.InvocationTargetException;

/**
 */
public class TestString {
    CLogger logger = Utils.getLogger(TestString.class);

    @Test
    public void test() throws InterruptedException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Platform.getUuid();
        DBGraph.printPath(VmNicVO.class, ImageVO.class);
    }
}
