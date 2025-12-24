package org.zstack.testlib.util


import java.util.concurrent.TimeUnit

class TestConfigUtils {
    private final static long DEFAULT_MESSAGE_TIMEOUT_SECS = TimeUnit.SECONDS.toMillis(25)

    static long getMessageTimeoutMillisConfig(){
        String msgTimeoutStr = System.getProperty("msgTimeoutMins")

        if(System.getProperty("maven.surefire.debug") != null && msgTimeoutStr == null){
            return TimeUnit.MINUTES.toMillis(30)
        }

        if(msgTimeoutStr == null || msgTimeoutStr.isEmpty()){
            return DEFAULT_MESSAGE_TIMEOUT_SECS
        }

        long msgTimeout
        try {
            msgTimeout = Long.parseLong(msgTimeoutStr)
        } catch (NumberFormatException e) {
            throw new RuntimeException("wrong format for msgTimeoutMins system property, it should be a number representing minutes", e)
        }

        return TimeUnit.MINUTES.toMillis(msgTimeout)
    }
}
