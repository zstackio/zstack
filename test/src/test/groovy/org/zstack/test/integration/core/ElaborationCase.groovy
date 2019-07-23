package org.zstack.test.integration.core

import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.header.errorcode.ElaborationVO
import org.zstack.header.errorcode.ElaborationVO_
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.ErrorCodeList
import org.zstack.header.identity.IdentityErrors
import org.zstack.sdk.ElaborationInventory
import org.zstack.sdk.GetElaborationCategoriesResult
import org.zstack.sdk.GetElaborationsResult
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import static org.zstack.core.Platform.operr
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
        testElaboration()
        env.create {
            testGetElaborationCategory()
            testGetElaboration()
            testGetMissedElaboration()
            testRefreshElaboration()
            testElaborationWithLongName()
            testElaborationWithUnknownFormatConversion()
            testErrorList()
        }
    }

    void testElaborationWithLongName() {
        def err = Platform.operr("host[uuid:%s, name:%s] is in status[%s], cannot perform required operation", Platform.uuid, "long long long long long long long long long host name", "Connecting") as ErrorCode
        assert err.elaboration != null
        assert err.elaboration.trim() == "错误信息: 物理机[long long long long long long long long long host name]正处于[Connecting]状态, 当前状态不允许进行该操作\n可能原因: 物理机正处于[Connecting]状态,当前状态不允许进行该操作\n操作建议: 请等待物理机退出[Connecting]状态"
    }

    void testElaboration() {
        def err = Platform.operr("certificate has expired or is not yet valid") as ErrorCode
        assert err.elaboration != null
        assert err.elaboration.trim() == "错误信息: 当前系统时间不在镜像仓库证书有效期内, 调整过镜像仓库服务器的系统时间，或者证书被修改\n可能原因: 调整过镜像仓库服务器的系统时间，或者证书被修改\n操作建议: 检查镜像服务器系统时间，或重置镜像仓库证书"

        err = Platform.operr("The state of vm[uuid:%s] is %s. Only these state[Running,Stopped] is allowed to update cpu or memory.", Platform.uuid, "Rebooting") as ErrorCode
        assert err.elaboration != null
        assert err.elaboration.trim() == "错误信息: 云主机的状态为Rebooting,只有状态[Running,Stopped]允许升级CPU/内存\n操作建议: 等待云主机进入[Running,Stopped]状态后，再升级CPU/内存"

        err = Platform.operr("test for missed error") as ErrorCode
        assert err.elaboration == null
        def missed = Q.New(ElaborationVO.class).eq(ElaborationVO_.errorInfo, "test for missed error").find() as ElaborationVO
        assert missed.distance > 0
        assert missed.repeats == 1

        err = Platform.operr("test for missed error") as ErrorCode
        assert err.elaboration == null
        missed = Q.New(ElaborationVO.class).eq(ElaborationVO_.errorInfo, "test for missed error").find() as ElaborationVO
        assert missed.distance > 0
        assert missed.repeats == 2

        err = Platform.err(IdentityErrors.INVALID_SESSION, "xxxxxxxxx") as ErrorCode
        assert err.elaboration != null
        missed = Q.New(ElaborationVO.class).eq(ElaborationVO_.errorInfo, "xxxxxxxxx").find() as ElaborationVO
        assert missed == null
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

    void testGetMissedElaboration() {
        def result = getMissedElaboration {
            repeats = 2
        } as List<ElaborationInventory>

        assert result.size() == 1
        assert result.get(0).errorInfo == "test for missed error"

        result = getMissedElaboration {
            startTime = "1999-01-01 10:00:00"
        } as List<ElaborationInventory>
        assert result.size() > 0

        result = getMissedElaboration {
            startTime = "1543593600000"   // 2018-12-01 00:00:00
        } as List<ElaborationInventory>
        assert result.size() > 0

        result = getMissedElaboration {
            startTime = "4099737600000"   // 2099-12-01 00:00:00
        } as List<ElaborationInventory>
        assert result.size() == 0

        result = getMissedElaboration {
            startTime = "2049-01-01 10:00:00"
        } as List<ElaborationInventory>
        assert result.size() == 0

        getMissedElaboration {
            startTime = "2019-01-01 300:00:00"
        }

        expect(AssertionError.class) {
            getMissedElaboration {
                startTime = "123456122222222222222222222"
            }
        }
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

    void testElaborationWithUnknownFormatConversion() {
        def err = Platform.operr("%!s(int=0) %!s(bytes.readOp=0)", "nowadays") as ErrorCode
        assert err.elaboration == null
        assert err.details == "%!s(int=0) %!s(bytes.readOp=0)"
        def missed = Q.New(ElaborationVO.class).eq(ElaborationVO_.errorInfo, "%!s(int=0) %!s(bytes.readOp=0)").find() as ElaborationVO
        assert !missed.matched
    }

    void testErrorList() {
        def list = new ArrayList<ErrorCode>()
        def err1 = operr("host[uuid:%s, name:%s] is in state[%s], cannot perform required operation", Platform.uuid, "host-1", "Maintenance") as ErrorCode
        def err2 = operr("host[uuid:%s, name:%s] is in state[%s], cannot perform required operation", Platform.uuid, "host-2", "Maintenance") as ErrorCode

        list.addAll([err1, err2])
        def errlist = new ErrorCodeList().causedBy(list)

        def err = operr(errlist, "unable to commit backup storage because: %s", err1.details)
        assert err.messages.message_cn == "物理机[host-1]正处于[Maintenance]状态, 当前状态不允许进行该操作,物理机[host-2]正处于[Maintenance]状态, 当前状态不允许进行该操作"
    }
}
