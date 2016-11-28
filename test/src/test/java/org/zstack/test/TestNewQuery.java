package org.zstack.test;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.header.query.QueryBuilder;
import org.zstack.header.query.QueryOp;
import org.zstack.header.storage.primary.APIQueryPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.APIQueryVmInstanceMsg;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.query.MysqlQueryBuilderImpl3;

/**
 */
public class TestNewQuery {
    ComponentLoader loader;
    MysqlQueryBuilderImpl3 query;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        query = loader.getComponent(MysqlQueryBuilderImpl3.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        APIQueryPrimaryStorageMsg msg = new APIQueryPrimaryStorageMsg();
        msg.addQueryCondition("uuid", QueryOp.EQ, Platform.getUuid());
        msg.addQueryCondition("attachedClusterUuids", QueryOp.NOT_IN, Platform.getUuid(), Platform.getUuid());
        query.start();
        QueryBuilder qb = (QueryBuilder) query;
        qb.query(msg, PrimaryStorageInventory.class);

        APIQueryVmInstanceMsg imsg = new APIQueryVmInstanceMsg();
        imsg.addQueryCondition("vmNics.uuid", QueryOp.EQ, Platform.getUuid());
        imsg.addQueryCondition("vmNics.l3NetworkUuid", QueryOp.NOT_EQ, Platform.getUuid());
        imsg.addQueryCondition("name", QueryOp.EQ, "vm");
        imsg.addQueryCondition("allVolumes.uuid", QueryOp.IN, Platform.getUuid());
        qb.query(imsg, VmInstanceInventory.class);
    }
}
