package com.zstack.utils.test;

import org.junit.Test;
import org.zstack.utils.ssh.SshException;
import org.zstack.utils.ssh.SshResult;

/**
 * Created by heathhose on 17-2-16.
 */
public class TestSshResult {
    @Test
    public void raiseExceptionIfFailed() throws Exception {
        SshResult rst = new SshResult();
        rst.setReturnCode(255);
        rst.setStdout("ssh:connect to host 172.20.11.90 port 322: Connection refused");

        StringBuilder sb = new StringBuilder("\nssh command failed");
//        sb.append(String.format("\ncommand: %s", commandToExecute));
        sb.append(String.format("\nreturn code: %s", 255));
        sb.append(String.format("\nstdout: %s", ""));
        sb.append(String.format("\nstderr: %s", "ssh:connect to host 172.20.11.90 port 322: Connection refused"));
        sb.append(String.format("\nexitErrorMessage: %s", ""));

        SshException se = new SshException(sb.toString());
        System.out.println(se.getMessage());

    }



}