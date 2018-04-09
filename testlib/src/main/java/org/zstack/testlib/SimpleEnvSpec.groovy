package org.zstack.testlib

class SimpleEnvSpec extends EnvSpec {
    @Override
    void delete() {
        resourcesNeedDeletion.each {
            logger.info("run delete() method on ${it.class}")
            it.delete()
        }
    }

    void use(Closure c) {
        def backup = Test.currentEnvSpec

        Test.currentEnvSpec = this
        create(c)
        delete()

        Test.currentEnvSpec = backup
    }

    @Override
    EnvSpec create(Closure cl = null) {
        adminLogin()

        installSimulatorHandlers()

        deploy()
        if (cl != null) {
            cl.delegate = this
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
        }

        return this
    }
}
