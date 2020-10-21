package org.zstack.authentication.checkfile;


import org.zstack.header.errorcode.ErrorCode;

import java.util.Map;

public interface FileVerificationFacade {

    Map<String, FileVerification> getAllFile();
    
    ErrorCode addVerificationFile(FileVerification fv);
    
    boolean anyCheckFilesExists(String node);

}
