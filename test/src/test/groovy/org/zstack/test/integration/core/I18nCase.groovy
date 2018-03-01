package org.zstack.test.integration.core

import com.google.common.collect.Lists
import org.zstack.core.Platform
import org.zstack.testlib.SubCase

class I18nCase extends SubCase {


    @Override
    void clean() {

    }

    @Override
    void setup() {

    }

    @Override
    void environment() {

    }

    @Override
    void test() {

        // prevent the case failing when some annotations are deleted
        // cancel annotation when testing i18n messsages

        // Locale en = new Locale("en_US")

        // test single quote
        // logger.info(Platform.toI18nString("not dest host found in db, can\u0027t send change password cmd to the host!", en, Lists.newArrayList()))
        // logger.info(Platform.toI18nString("ecs instance [%s] start isn\\u0027t finish, status is still [%s]", en, "aaaaaa", "bbbbbb"))

        // test %d
        // logger.info(Platform.toI18nString("invalid volume IOPS[%s] is larger than %d", en, "cccccc",123))
        // logger.info(Platform.toI18nString("unable to connect to KVM[ip:%s, username:%s, sshPort:%d ] to do DNS check, please check if username/password is wrong; %s", en, "dddddd", "eeeeee", 456, "ffffff"))

        // test zh_CN
        // logger.info(Platform.toI18nString("modify-ecs-[%s]-console-vnc-password-failed, due to [code: %s, details: %s]", null, "gggggg", "hhhhhh", "iiiiii"))

    }
}
