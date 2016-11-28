package org.zstack.test.core.db;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.zone.ZoneVO;
import org.zstack.test.BeanConstructor;
import org.zstack.test.DBUtil;

public class TestDatabaseFacade {
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        dbf = loader.getComponent(DatabaseFacade.class);
        DBUtil.reDeployDB();
    }

    @Test
    public void test() {
        ZoneVO vo = new ZoneVO();
        vo.setUuid(Platform.getUuid());
        vo.setName("Test persist");
        vo.setDescription("Test persist");
        vo.setType("TestType");
        vo = dbf.persistAndRefresh(vo);

        ZoneVO vo1 = dbf.findByUuid(vo.getUuid(), ZoneVO.class);
        Assert.assertNotNull(vo1);

        dbf.remove(vo1);
        vo1 = dbf.findByUuid(vo.getUuid(), ZoneVO.class);
        Assert.assertNull(vo1);

        vo = new ZoneVO();
        vo.setUuid(Platform.getUuid());
        vo.setName("Test persist2");
        vo.setDescription("Test persist");
        vo.setType("TestType");
        vo = dbf.persistAndRefresh(vo);
        vo1 = dbf.findByUuid(vo.getUuid(), ZoneVO.class);
        Assert.assertEquals(vo1.getName(), "Test persist2");
    }
}
