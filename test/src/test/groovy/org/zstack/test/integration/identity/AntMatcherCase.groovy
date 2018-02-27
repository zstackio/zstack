package org.zstack.test.integration.identity

import org.junit.Test
import org.springframework.util.AntPathMatcher

class AntMatcherCase {
    @Test
    void test() {
        def matcher = new AntPathMatcher()
        def out = matcher.match("!abcd", "abcd")
        println(out)
    }
}
