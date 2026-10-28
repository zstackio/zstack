package org.zstack.test.integration.db.schema

import groovy.sql.Sql
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
        testZnsSegmentOperationContextUpgradeSchema()
        testHistoricalAffinityGroupZoneUpgrade()
    }

    void testLoadBalancerListenerDataPlaneUpgradeSchema() {
        String upgradeSchemaDir = Paths.get("../conf/db/upgrade").toAbsolutePath().normalize().toString()
        File schema = new File(upgradeSchemaDir + "/V5.5.31__schema.sql")
        assert schema.exists()

        String sql = schema.text
        assert sql.contains("CALL ADD_COLUMN('LoadBalancerListenerVO', 'data_plane', 'VARCHAR(32)', 1, NULL)")
        assert sql.contains("CALL ADD_COLUMN('LoadBalancerListenerVO', 'forward_mode', 'VARCHAR(32)', 1, NULL)")
        assert sql.contains("WHERE protocol = 'udp' AND data_plane IS NULL")
        assert sql.contains("SET data_plane = 'haproxy'")
        assert sql.contains("WHERE data_plane IS NULL")
    }

    void testZnsSegmentOperationContextUpgradeSchema() {
        String upgradeSchemaDir = Paths.get("../conf/db/upgrade").toAbsolutePath().normalize().toString()
        File schema = new File(upgradeSchemaDir + "/V5.5.38__schema.sql")
        assert schema.exists()

        String sql = schema.text
        [
                "operationCapabilityName": "VARCHAR(64)",
                "operationCapabilityContract": "VARCHAR(32)",
                "operationCapabilityEpoch": "BIGINT",
                "operationProviderIdentity": "VARCHAR(128)",
                "operationProfileIdentity": "VARCHAR(128)",
                "operationAdapterIdentity": "VARCHAR(128)"
        ].each { String column, String type ->
            assert sql.contains("CALL ADD_COLUMN('ZnsSegmentRefVO', '${column}', '${type}', 1, NULL)")
        }
    }

    void testHistoricalAffinityGroupZoneUpgrade() {
        Properties properties = new Properties()
        getClass().classLoader.getResourceAsStream("zstack.properties").withCloseable {
            properties.load(it)
        }
        Sql db = Sql.newInstance(
                System.getProperty("DB.url", properties.getProperty("DB.url")),
                System.getProperty("DB.user", properties.getProperty("DB.user")),
                System.getProperty("DB.password", properties.getProperty("DB.password")),
                "com.mysql.jdbc.Driver")
        try {
            [
                    ZoneVO: "uuid varchar(32) PRIMARY KEY",
                    VmInstanceVO: "uuid varchar(32) PRIMARY KEY, zoneUuid varchar(32)",
                    AffinityGroupVO: "uuid varchar(32) PRIMARY KEY, zoneUuid varchar(32)",
                    AffinityGroupUsageVO: "affinityGroupUuid varchar(32), resourceUuid varchar(32)",
                    VmSchedulingRuleGroupVO: "uuid varchar(32) PRIMARY KEY, srcUuid varchar(32), zoneUuid varchar(32)"
            ].each { String table, String columns ->
                db.execute("CREATE TEMPORARY TABLE zstack.${table} (${columns})".toString())
            }
            db.execute("INSERT INTO zstack.ZoneVO VALUES ('zone-a'), ('zone-b')")
            db.execute("INSERT INTO zstack.VmInstanceVO VALUES ('vm-a', 'zone-a'), ('vm-b', 'zone-b')")
            db.execute("INSERT INTO zstack.AffinityGroupVO VALUES ('empty', NULL), ('a', NULL), ('b', NULL), ('mixed', NULL), ('preserved', 'zone-b')")
            db.execute("INSERT INTO zstack.AffinityGroupUsageVO VALUES ('a', 'vm-a'), ('b', 'vm-b'), ('mixed', 'vm-a'), ('mixed', 'vm-b'), ('preserved', 'vm-a')")
            db.execute("INSERT INTO zstack.VmSchedulingRuleGroupVO VALUES ('group-a', 'a', NULL), ('group-b', 'b', NULL), ('group-mixed', 'mixed', NULL), ('group-empty', 'empty', NULL), ('group-preserved', 'preserved', NULL), ('group-own-zone', 'a', 'zone-b')")

            File schema = Paths.get("../conf/db/upgrade/V5.6.0__schema.sql").toFile()
            List<String> upgrades = schema.text.split(';').findAll {
                it.contains('UPDATE `zstack`.`AffinityGroupVO`') ||
                        it.contains('UPDATE `zstack`.`VmSchedulingRuleGroupVO`')
            }
            2.times {
                upgrades.each { db.execute(it) }
                def affinityZones = db.rows("SELECT uuid, zoneUuid FROM zstack.AffinityGroupVO").collectEntries { [(it.uuid): it.zoneUuid] }
                def groupZones = db.rows("SELECT uuid, zoneUuid FROM zstack.VmSchedulingRuleGroupVO").collectEntries { [(it.uuid): it.zoneUuid] }
                assert affinityZones == [empty: null, a: 'zone-a', b: 'zone-b', mixed: null, preserved: 'zone-b'] :
                        "Historical affinity zones must follow unambiguous members and preserve existing values: actual=${affinityZones}"
                assert groupZones == ['group-a': 'zone-a', 'group-b': 'zone-b', 'group-mixed': null, 'group-empty': null, 'group-preserved': 'zone-b', 'group-own-zone': 'zone-b'] :
                        "Historical scheduling groups must inherit only missing zones: actual=${groupZones}"
            }

            db.execute("DELETE FROM zstack.ZoneVO WHERE uuid = 'zone-b'")
            upgrades.each { db.execute(it) }
            assert db.firstRow("SELECT zoneUuid FROM zstack.AffinityGroupVO WHERE uuid = 'empty'").zoneUuid == 'zone-a' :
                    "An empty historical group must use the sole zone"
            assert db.firstRow("SELECT zoneUuid FROM zstack.VmSchedulingRuleGroupVO WHERE uuid = 'group-empty'").zoneUuid == 'zone-a' :
                    "The scheduling group must also receive the sole zone"
        } finally {
            db.close()
        }
    }
}
