package org.zstack.test.integration.configuration.systemTag

import org.zstack.compute.host.HostSystemTags
import org.zstack.header.tag.SystemTagInventory
import org.zstack.header.tag.AbstractSystemTagLifeCycleListener
import org.zstack.header.tag.AbstractSystemTagOperationJudger
import org.zstack.tag.SystemTag
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class SystemTagQuickRecreateCase extends SubCase {
    EnvSpec env

    @Override
    void setup() { useSpring(KvmTest.springSpec) }

    @Override
    void environment() { env = Env.oneVmBasicEnv() }

    @Override
    void test() {
        env.create {
            def host = queryHost {}[0]
            def tag = HostSystemTags.SYSTEM_SERIAL_NUMBER
            tag.delete(host.uuid)
            def events = []
            def listener = new AbstractSystemTagLifeCycleListener() {
                @Override
                void tagCreated(SystemTagInventory inv) {
                    if (inv.resourceUuid == host.uuid) { events.add("created:${inv.tag}".toString()) }
                }
                @Override
                void tagDeleted(SystemTagInventory inv) {
                    if (inv.resourceUuid == host.uuid) { events.add("deleted:${inv.tag}".toString()) }
                }
            }
            def judger = new AbstractSystemTagOperationJudger() {
                @Override
                void tagPreDeleted(SystemTagInventory inv) {
                    if (inv.resourceUuid == host.uuid) { events.add("pre-delete:${inv.tag}".toString()) }
                }
            }
            def create = { String value, boolean recreate, boolean quick ->
                def creator = tag.newSystemTagCreator(host.uuid)
                creator.inherent = false
                creator.recreate = recreate
                creator.quickRecreate = quick
                creator.setTagByTokens([(HostSystemTags.SYSTEM_SERIAL_NUMBER_TOKEN): value])
                creator.create()
            }
            tag.installLifeCycleListener(listener)
            tag.installJudger(judger)
            try {
                def original = create("keep", false, true)
                events.clear()
                assert create("keep", true, true).uuid == original.uuid
                assert events.empty

                // LIKE matching must still remove different values while retaining the exact match.
                create("old", false, true)
                events.clear()
                assert create("keep", true, true).uuid == original.uuid
                assert events == ["pre-delete:systemSerialNumber::old", "deleted:systemSerialNumber::old"]
                assert tag.getTags(host.uuid).findAll { it in ["systemSerialNumber::keep", "systemSerialNumber::old"] } == ["systemSerialNumber::keep"]

                events.clear()
                create("new", true, true)
                assert events == ["pre-delete:systemSerialNumber::keep", "deleted:systemSerialNumber::keep", "created:systemSerialNumber::new"]

                events.clear()
                create("new", true, false)
                assert events == ["pre-delete:systemSerialNumber::new", "deleted:systemSerialNumber::new", "created:systemSerialNumber::new"]
            } finally {
                [lifeCycleListeners: listener, judgers: judger].each { name, extension ->
                    def field = SystemTag.class.getDeclaredField(name)
                    field.accessible = true
                    (field.get(tag) as List).remove(extension)
                }
                tag.delete(host.uuid)
            }
        }
    }

    @Override
    void clean() { env.delete() }
}
