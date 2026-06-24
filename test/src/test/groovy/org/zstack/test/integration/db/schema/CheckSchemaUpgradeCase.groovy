package org.zstack.test.integration.db.schema

import org.apache.commons.io.FileUtils
import org.zstack.testlib.SubCase

import java.nio.file.Paths


/**
 * Created by Qi Le on 2022/6/6
 */
class CheckSchemaUpgradeCase extends SubCase {
    File neoSchema

    @Override
    void clean() {
        FileUtils.deleteQuietly(neoSchema)
    }

    @Override
    void setup() {
        String upgradeSchemaDir = Paths.get("../conf/db/upgrade").toAbsolutePath().normalize().toString()
        neoSchema = new File(upgradeSchemaDir + "/V100.100.100__schema.sql")
        FileUtils.deleteQuietly(neoSchema)
        FileUtils.touch(neoSchema)
        FileUtils.deleteQuietly(neoSchema)
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        testLoadBalancerListenerDataPlaneUpgradeSchema()
    }

    void testLoadBalancerListenerDataPlaneUpgradeSchema() {
        String upgradeSchemaDir = Paths.get("../conf/db/upgrade").toAbsolutePath().normalize().toString()
        File schema = new File(upgradeSchemaDir + "/V5.5.28__schema.sql")
        assert schema.exists()

        String sql = schema.text
        assert sql.contains("CALL ADD_COLUMN('LoadBalancerListenerVO', 'data_plane', 'VARCHAR(32)', 1, NULL)")
        assert sql.contains("CALL ADD_COLUMN('LoadBalancerListenerVO', 'forward_mode', 'VARCHAR(32)', 1, NULL)")
        assert sql.contains("WHERE protocol = 'udp' AND data_plane IS NULL")
        assert sql.contains("SET data_plane = 'haproxy'")
        assert sql.contains("WHERE data_plane IS NULL")
    }
}
