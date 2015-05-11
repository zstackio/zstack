package com.zstack.utils.test;

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

public class TestURI {

    @Test
    public void test() {
        String uri1 = "jar:file:/home/zstack";
        String uri2 = "file:/home/zstack";
        
        try {
            URI u1 = new URI(uri1);
            URI u2 = new URI(uri2);
            System.out.println(u1.getPort());
            System.out.println(u2.getPath());
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
