package org.zstack.test.integration.core

import org.springframework.beans.factory.annotation.Autowired
import org.zstack.core.errorcode.ErrorFacade
import org.zstack.core.errorcode.ErrorFacadeImpl
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.ErrorCodeList
import org.zstack.testlib.SubCase

import static org.zstack.core.Platform.argerr
import static org.zstack.core.Platform.operr

/**
 * Created by MaJin on 2017-06-22.
 */
class ErrorCodeHashCase extends SubCase{
    @Override
    void setup() {
        INCLUDE_CORE_SERVICES = false
    }

    @Override
    void environment() {

    }

    @Override
    void test() {
        ErrorFacade errf = bean(ErrorFacade.class)
        ErrorCode err1, err2, err3
        ErrorCodeList errl1, errl2, errl3, errl4
        err1 = operr("test error")
        err2 = operr("test error")
        err3 = argerr("test error")

        assert err1.hashCode() == err1.hashCode()
        assert err1.hashCode() == err2.hashCode()
        assert err1.hashCode() != err3.hashCode()
        assert Objects.equals(err1, err1)
        assert Objects.equals(err1, err2)
        assert !Objects.equals(err1, err3)


        errl1 = errf.stringToOperationError("test error list", [err1, err2])
        errl2 = errf.stringToOperationError("test error list", [err1, err2])
        errl3 = errf.stringToOperationError("test error list", [])
        errl4 = errf.stringToOperationError("test error list", [])

        assert errl1.hashCode() == errl1.hashCode()
        assert errl1.hashCode() == errl2.hashCode()
        assert errl1.hashCode() != errl3.hashCode()
        assert errl3.hashCode() == errl4.hashCode()
        assert Objects.equals(errl1, errl1)
        assert Objects.equals(errl1, errl2)
        assert !Objects.equals(errl1, errl3)
        assert Objects.equals(errl3, errl4)

        err1.setDetails("test error list")
        assert err1.hashCode() == errl3.hashCode()
        assert Objects.equals(err1, errl3)

        errl3.setCauses(null)
        assert errl3.hashCode() == errl4.hashCode()
        assert Objects.equals(errl3, errl4)
        assert err1.hashCode() == errl3.hashCode()
        assert Objects.equals(err1, errl3)
        
        errl4.setCauses(null)
        assert errl3.hashCode() == errl4.hashCode()
        assert Objects.equals(errl3, errl4)

        assert errl1.hashCode() != errl3.hashCode()
        assert !Objects.equals(errl1, errl3)
    }

    @Override
    void clean() {

    }

}
