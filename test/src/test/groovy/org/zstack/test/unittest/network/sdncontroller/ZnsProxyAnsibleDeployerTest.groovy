package org.zstack.test.unittest.network.sdncontroller

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.zstack.core.CoreGlobalProperty
import org.zstack.core.ansible.AnsibleFacade
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.sdnController.ZnsProxyGlobalProperty
import org.zstack.sdnController.znsproxy.ZnsProxyAnsibleDeployer

class ZnsProxyAnsibleDeployerTest {
    private boolean originalUnitTestOn

    @Before
    void setUp() {
        originalUnitTestOn = CoreGlobalProperty.UNIT_TEST_ON
    }

    @After
    void tearDown() {
        CoreGlobalProperty.UNIT_TEST_ON = originalUnitTestOn
    }

    @Test
    void testRegisteredAsIndependentComponent() {
        URL config = getClass().classLoader.getResource("springConfigXml/sdnController.xml")
        assert config != null

        def beans = new XmlSlurper(false, true).parse(config.openStream())
        beans.declareNamespace(zstack: "http://zstack.org/schema/zstack")
        def deployer = beans.bean.find { it.@id.text() == "ZnsProxyAnsibleDeployer" }
        assert deployer.@class.text() == ZnsProxyAnsibleDeployer.name
        assert deployer.'zstack:plugin'.'zstack:extension'.any {
            it.@interface.text() == "org.zstack.header.Component"
        }
    }

    @Test
    void testUnitTestModeSkipsModuleDeployment() {
        CoreGlobalProperty.UNIT_TEST_ON = true
        RecordingAnsibleFacade facade = new RecordingAnsibleFacade()
        ZnsProxyAnsibleDeployer deployer = deployerWith(facade)

        assert deployer.start()
        assert facade.deployCount == 0
    }

    @Test
    void testStartDeploysZnsProxyModule() {
        CoreGlobalProperty.UNIT_TEST_ON = false
        RecordingAnsibleFacade facade = new RecordingAnsibleFacade()
        ZnsProxyAnsibleDeployer deployer = deployerWith(facade)

        assert deployer.start()
        assert facade.deployCount == 1
        assert facade.modulePath == ZnsProxyGlobalProperty.ANSIBLE_MODULE_PATH
        assert facade.playbookName == ZnsProxyGlobalProperty.ANSIBLE_PLAYBOOK_NAME
    }

    @Test
    void testModuleDeploymentFailureIsPropagated() {
        CoreGlobalProperty.UNIT_TEST_ON = false
        RecordingAnsibleFacade facade = new RecordingAnsibleFacade(
                failure: new CloudRuntimeException("ansible/znsproxy is missing"))
        ZnsProxyAnsibleDeployer deployer = deployerWith(facade)

        try {
            deployer.start()
        } catch (CloudRuntimeException e) {
            assert e.message == "ansible/znsproxy is missing"
            return
        }
        assert false: "module deployment failure must fail component startup"
    }

    private static ZnsProxyAnsibleDeployer deployerWith(AnsibleFacade facade) {
        ZnsProxyAnsibleDeployer deployer = new ZnsProxyAnsibleDeployer()
        def field = ZnsProxyAnsibleDeployer.getDeclaredField("asf")
        field.accessible = true
        field.set(deployer, facade)
        return deployer
    }

    private static class RecordingAnsibleFacade implements AnsibleFacade {
        int deployCount
        String modulePath
        String playbookName
        CloudRuntimeException failure

        @Override
        void deployModule(String modulePath, String playbookName) {
            deployCount++
            this.modulePath = modulePath
            this.playbookName = playbookName
            if (failure != null) {
                throw failure
            }
        }

        @Override
        boolean isModuleChanged(String playbookName) {
            return false
        }

        @Override
        Map<String, String> getVariables() {
            return Collections.emptyMap()
        }

        @Override
        String getPublicKey() {
            return null
        }

        @Override
        String getPrivateKey() {
            return null
        }
    }
}
