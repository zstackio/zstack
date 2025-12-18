package org.zstack.test.integration.core

import org.apache.commons.lang.LocaleUtils
import org.zstack.core.Platform
import org.zstack.sdk.GetElaborationCategoriesResult
import org.zstack.sdk.GetElaborationsResult
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import static org.zstack.core.Platform.errorCodeElaboration
/**
 * Created by mingjian.deng on 2018/11/28.*/
class ElaborationCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        INCLUDE_CORE_SERVICES = false
    }

    @Override
    void environment() {
        env = new EnvSpec()
    }

    @Override
    void test() {
        def oldLocale = Platform.locale
        Platform.locale = LocaleUtils.toLocale("zh_CN")
        onCleanExecute {
            Platform.locale = oldLocale
        }

        testElaboration()
        env.create {
            testGetElaborationCategory()
            testGetElaboration()
            testRefreshElaboration()
            testElaborationWithLongName()
            testElaborationLanguageEnglish()
            testElaborationLanguageNotSupport()
        }
    }

    void testElaborationWithLongName() {
        def err = errorCodeElaboration("host[uuid:%s, name:%s] is in status[%s], cannot perform required operation", Platform.uuid, "long long long long long long long long long host name", "Connecting")
        assert err.trim() == "物理机 [long long long long long long long long long host name] 正处于 [Connecting] 状态，当前状态不允许进行该操作。"
    }

    void testElaboration() {
        def err = errorCodeElaboration("certificate has expired or is not yet valid")
        assert err.trim() == "当前系统时间不在镜像仓库证书有效期内，可能因为镜像仓库服务器的系统时间被调整，或者证书被修改。"

        err = errorCodeElaboration("The state of vm[uuid:%s] is %s. Only these state[Running,Stopped] is allowed to update cpu or memory.", Platform.uuid, "Rebooting")
        assert err.trim() == "云主机的状态为 Rebooting，只有状态 [Running，Stopped] 允许升级 CPU/内存。"

        err = errorCodeElaboration("test for missed error")
        assert err == null // no matched elaboration
    }

    void testElaborationLanguageEnglish() {
        Locale originLocale = Platform.locale
        Platform.locale = Locale.US
        testElaborationEnglish()
        Platform.locale = originLocale
    }

    void testElaborationLanguageNotSupport() {
        Locale originLocale = Platform.locale
        Platform.locale = Locale.FRANCE
        testElaborationEnglish()
        Platform.locale = originLocale
    }

    void testElaborationEnglish() {
        def err = errorCodeElaboration("certificate has expired or is not yet valid")
        assert err.trim() == "The current system time has expired for ImageStore certificate. Possible reason: ImageStore server system time or certificate is modified."

        err = errorCodeElaboration("The state of vm[uuid:%s] is %s. Only these state[Running,Stopped] is allowed to update cpu or memory.", Platform.uuid, "Rebooting")
        assert err != null
        assert err.trim() == "Only VMs with the status [Running, Stopped] support CPU/memory update. Current status: Rebooting."
    }

    void testGetElaborationCategory() {
        def result = getElaborationCategories {
            sessionId = adminSession()
        } as GetElaborationCategoriesResult
        assert result.categories.size() > 2
    }

    void testGetElaboration() {
        def result = getElaborations {
            category = "ACCOUNT"
        } as GetElaborationsResult

        assert result.contents.size() > 0

        result = getElaborations {
            category = "ACCOUNT"
            code = "1000"
        } as GetElaborationsResult

        assert result.contents.size() > 0

        result = getElaborations {
            category = "ACCOUNT"
            code = "9999"
        } as GetElaborationsResult

        assert result.contents.size() == 0

        result = getElaborations {
            regex = "certificate has expired or is not yet valid"
        } as GetElaborationsResult

        assert result.contents.size() == 1

        result = getElaborations {
            regex = "certificate"
        } as GetElaborationsResult

        assert result.contents.size() == 0
    }

    void testRefreshElaboration() {
        def result = getElaborationCategories {
            sessionId = adminSession()
        } as GetElaborationCategoriesResult

        def size = result.categories.size()
        reloadElaboration {
        }

        result = getElaborationCategories {
            sessionId = adminSession()
        } as GetElaborationCategoriesResult

        assert size == result.categories.size()
    }
}
