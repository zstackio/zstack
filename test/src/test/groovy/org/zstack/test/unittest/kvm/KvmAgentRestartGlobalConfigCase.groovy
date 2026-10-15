package org.zstack.test.unittest.kvm

import org.junit.Test
import org.zstack.core.config.GlobalConfig
import org.zstack.core.config.GlobalConfigException
import org.zstack.core.config.GlobalConfigFacadeImpl
import org.zstack.kvm.KVMGlobalConfig

import javax.xml.bind.JAXBContext
import java.lang.reflect.Field

import static org.junit.Assert.*

class KvmAgentRestartGlobalConfigCase {
    private static Object invoke(Object target, String name, Object... arguments) {
        def method = target.class.declaredMethods.find {
            it.name == name && it.parameterTypes.length == arguments.length
        }
        assertNotNull(method)
        method.accessible = true
        return method.invoke(target, arguments)
    }

    @Test
    void restartTimeoutIsRegisteredWithEffectiveKvmXml() {
        Field configField = KVMGlobalConfig.class.getField("AGENT_RESTART_TIMEOUT")
        GlobalConfig original = (GlobalConfig) configField.get(null)
        try {
            // Use a fresh field value so linking cannot mutate the running test environment.
            configField.set(null, new GlobalConfig("kvm", "agentRestartTimeout"))
            GlobalConfigFacadeImpl facade = new GlobalConfigFacadeImpl()
            Field context = GlobalConfigFacadeImpl.class.getDeclaredField("context")
            context.accessible = true
            context.set(facade, JAXBContext.newInstance("org.zstack.core.config.schema"))

            // Exercise the real XML -> Java definition -> link path without a database.
            Class initializerClass = Class.forName(
                    'org.zstack.core.config.GlobalConfigFacadeImpl$1GlobalConfigInitializer')
            def constructor = initializerClass.getDeclaredConstructor(GlobalConfigFacadeImpl.class)
            constructor.accessible = true
            Object initializer = constructor.newInstance(facade)
            Field fields = initializerClass.getDeclaredField("globalConfigFields")
            fields.accessible = true
            ((List) fields.get(initializer)).add(configField)

            URL xml = getClass().classLoader.getResource("globalConfig/kvm.xml")
            assertNotNull(xml)
            invoke(initializer, "parseConfig", new File(xml.toURI()))
            invoke(initializer, "loadConfigFromJava")
            invoke(initializer, "link")

            GlobalConfig config = KVMGlobalConfig.AGENT_RESTART_TIMEOUT
            assertTrue(config.isLinked())
            assertEquals(Integer.class.name, config.getType())
            assertEquals(Integer.valueOf(120), config.value(Integer.class))
            assertEquals(Integer.valueOf(120), config.defaultValue(Integer.class))
            config.validate("31")
            try {
                config.validate("30")
                fail("Restart timeout must reject values below 31 seconds")
            } catch (GlobalConfigException expected) {
                assertTrue(expected.message.contains("31"))
            }
        } finally {
            configField.set(null, original)
        }
    }
}
