package com.zstack.utils.test;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.*;

public class TestURI {

    @Test
    public void test() throws URISyntaxException, UnsupportedEncodingException, MalformedURLException {
        String password = "zstackqwe:!@#";
        String url = String.format("root:%s@172.16.36.184/", password);
        int at = url.lastIndexOf("@");
        String userinfo = url.substring(0, at);
        System.out.println(userinfo);
        String rest = url.substring(at+1, url.length());
        System.out.println(rest);
        System.out.println(userinfo.split(":")[0]);
    }

}
