package com.zstack.utils.test;

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

public class TestURI {

    @Test
    public void test() throws URISyntaxException {
        URI u1 = new URI("ssh://root:zstackqwe:!@#@172.16.36.184/");
        System.out.println(u1.getAuthority());
        System.out.println(u1.getHost());
    }

}
