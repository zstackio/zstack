package org.zstack.test.integration.core.ansible

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Property
import org.apache.logging.log4j.core.layout.PatternLayout
import org.junit.Test
import org.zstack.core.ansible.AnsibleBasicArguments
import org.zstack.utils.gson.JSONObjectUtil

class AnsibleBasicArgumentsLogSafetyTest {
    @Test
    void traceMasksPasswordsWithoutChangingExecutionArguments() {
        String password = "ansible-log-test-" + UUID.randomUUID()
        AnsibleBasicArguments arguments = new AnsibleBasicArguments()
        arguments.remoteUser = "ansible-log-test"
        arguments.remotePort = "22"
        arguments.remotePass = password

        List<String> messages = []
        def layout = PatternLayout.newBuilder().withPattern("%m%n%throwable").build()
        def appender = new AbstractAppender("ansible-arguments-log-test", null, layout, false, Property.EMPTY_ARRAY) {
            @Override
            void append(LogEvent event) {
                messages.add(layout.toSerializable(event))
            }
        }
        Logger logger = (Logger) LogManager.getLogger(AnsibleBasicArguments.class)
        Level oldLevel = logger.level
        boolean oldAdditivity = logger.additive
        appender.start()
        logger.additive = false
        logger.addAppender(appender)
        logger.level = Level.TRACE
        try {
            Map<String, Object> executionArguments = arguments.toArgumentMap()

            assert messages.size() == 1
            assert !messages[0].contains(password)
            String prefix = "object after rehashed: "
            assert messages[0].startsWith(prefix)
            Map loggedArguments = JSONObjectUtil.toObject(messages[0].substring(prefix.length()).trim(), LinkedHashMap.class)
            assert loggedArguments.remotePass == "*****"
            assert loggedArguments.remote_pass == "*****"
            assert loggedArguments.remote_user == "ansible-log-test"
            assert loggedArguments.remote_port == "22"
            assert executionArguments.remotePass == password
            assert executionArguments.remote_pass == password
            assert executionArguments.remote_user == "ansible-log-test"
            assert executionArguments.remote_port == "22"
            assert arguments.remotePass == password
            assert arguments.remote_pass == password
        } finally {
            logger.removeAppender(appender)
            logger.additive = oldAdditivity
            logger.level = oldLevel
            appender.stop()
        }
    }
}
