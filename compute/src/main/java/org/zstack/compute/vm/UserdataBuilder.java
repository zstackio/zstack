package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;

import java.util.*;

/**
 * Created by xing5 on 2016/4/22.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class UserdataBuilder {
    @Autowired
    private DatabaseFacade dbf;

    private String sshkeyRootPassword(String sshKey, String rootPassword) {
        StringBuilder sb = new StringBuilder("#cloud-config");
        if (sshKey != null) {
            sb.append("\nssh_authorized_keys:");
            sb.append(String.format("\n  - %s", sshKey));
            sb.append("\ndisable_root: false");
        }
        if (rootPassword != null) {
            sb.append("\nchpasswd:");
            sb.append("\n  list: |");
            sb.append(String.format("\n    root:%s", rootPassword));
            sb.append("\n  expire: False");
        }

        return sb.toString();
    }

    public String buildByVmUuid(String vmUuid) {
        String userdata = VmSystemTags.USERDATA.getTokenByResourceUuid(vmUuid, VmSystemTags.USERDATA_TOKEN);
        if (userdata != null) {
            userdata = new String(Base64.getDecoder().decode(userdata.getBytes()));
            return userdata;
        }

        String sshKey = VmSystemTags.SSHKEY.getTokenByResourceUuid(vmUuid, VmSystemTags.SSHKEY_TOKEN);
        String rootPassword = VmSystemTags.ROOT_PASSWORD.getTokenByResourceUuid(vmUuid, VmSystemTags.ROOT_PASSWORD_TOKEN);
        if (sshKey == null && rootPassword == null) {
            return null;
        }

        return sshkeyRootPassword(sshKey, rootPassword);
    }

    public Map<String, String> buildByVmUuids(List<String> vmUuids) {
        Map<String, String> ret = new HashMap<String, String>();
        for (String vmUuid : vmUuids) {
            ret.put(vmUuid, buildByVmUuid(vmUuid));
        }

        return ret;
    }
}
