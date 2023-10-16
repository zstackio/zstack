package org.zstack.sugonSdnController.controller.api;

import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.List;

public class TfCommands {
    public static final String TF_GET_DAEMON = "/fqname-to-id";
    public static final String TF_GET_DAEMON_DETAIL = "/domain/.*";
    public static final String TF_GET_PROJECT = "/project/.*";
    public static final String TF_CREATE_PROJECT = "/projects";
    public static final String TF_GET_NETWORK = "/virtual-network/.*";
    public static final String TF_CREATE_NETWORK = "/virtual-networks";
    public static final String TF_GET_VM = "/virtual-machine/.*";
    public static final String TF_CREATE_VM = "/virtual-machines";
    public static final String TF_GET_VMI = "/virtual-machine-interface/.*";
    public static final String TF_CREATE_VMI = "/virtual-machine-interfaces";
    public static final String TEST_DOMAIN_UUID = "cf8107ad-eee6-4a54-be5e-c05c96a9d552";
    public static final String TEST_PROJECT_UUID = "36c27e8f-f05c-4780-bf6d-2fa65700f22e";
    public static final String TEST_L2_UUID = "1eca756e-b935-45ed-a2bc-a3ebba535f7f";
    public static final String TEST_VM_UUID = "35c27fde-7f67-4c78-b80a-56ae1eefcb5b";
    public static final String TEST_VMI_UUID = "a581ee83-f5a5-4755-bedf-8d51f235da52";

    public static class TfCmd {
        public String toString() {
            return JSONObjectUtil.toJsonString(this);
        }
    }

    public static class TfRsp {
    }

    public static class GetDomainCmd extends TfCmd {
        public String type;
        public List<String> fq_name = new ArrayList<>();
    }

    public static class GetDomainRsp extends TfRsp {
        public String uuid;
    }

    public static class GetProjectCmd extends TfCmd {
        public String uuid;
    }

}

