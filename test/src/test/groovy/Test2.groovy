import org.junit.Test

/**
 * Created by xing5 on 2017/2/17.
 */
class Test2 {

    @Test
    void test() {
        def lst = ["t":[1, 2, 3]]
        def p = { v ->
            println("${v.class}")
        }

        p(lst)
    }
}
