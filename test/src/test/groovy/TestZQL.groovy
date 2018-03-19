import org.junit.Test
import org.zstack.zql.ZQL

class TestZQL {
    @Test
    void test() {
        String text = "query vm.uuid"
        ZQL zql = ZQL.fromString(text)
        println(zql.toString())
    }
}
