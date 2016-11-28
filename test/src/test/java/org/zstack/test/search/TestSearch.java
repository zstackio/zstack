package org.zstack.test.search;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.search.ESTuple;
import org.zstack.search.SearchQuery;
import org.zstack.test.Api;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

public class TestSearch {
    CLogger logger = Utils.getLogger(TestSearch.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("SearchManager.xml").addXml("AccountManager.xml").addXml("VmInstanceManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() {
        SearchQuery<VmInstanceInventory> q = new SearchQuery<VmInstanceInventory>(VmInstanceInventory.class);
        q.setSize(2);
        List<VmInstanceInventory> invs = q.list();
        for (VmInstanceInventory vm : invs) {
            logger.debug(Utils.getFieldPrinter().print(vm));
        }

        SearchQuery<VmInstanceInventory> q1 = new SearchQuery<VmInstanceInventory>(VmInstanceInventory.class);
        q1.select("uuid", "name", "vmNics");
        List<ESTuple> tuples = q1.listTuple();
        for (ESTuple t : tuples) {
            logger.debug(t.get(0));
            logger.debug(t.get("name"));
            ArrayList<VmNicInventory> nics = t.get("vmNics", ArrayList.class, VmNicInventory.class);
            logger.debug(nics.toString());
        }
    }

}
