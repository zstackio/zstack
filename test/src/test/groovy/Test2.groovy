import org.zstack.testlib.Test

/**
 * Created by xing5 on 2017/2/17.
 */
class Test2 extends Test {
    @Override
    void setup() {
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        boolean ret = retryInSecs(3, 1) {
            // do something
            return true
        }

        assert ret

        ret = retryInSecs(3) {
            return false
        }

        assert !ret
    }
}
