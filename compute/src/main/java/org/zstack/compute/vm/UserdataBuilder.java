package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        Map<String, List<String>> userdata = VmSystemTags.USERDATA.getTags(vmUuids);
        for (Map.Entry<String, List<String>> e : userdata.entrySet()) {
            ret.put(e.getKey(), e.getValue().get(0));
        }

        // all vms have userdata
        if (ret.size() == vmUuids.size()) {
            return ret;
        }

        List<String> leftover = new ArrayList<String>();
        for (String uuid : vmUuids) {
            if (!ret.containsKey(uuid)) {
                leftover.add(uuid);
            }
        }

        Map<String, List<String>> sshKeys = VmSystemTags.SSHKEY.getTags(leftover);
        Map<String, List<String>> rootPasswords = VmSystemTags.ROOT_PASSWORD.getTags(leftover);
        for (String uuid : vmUuids) {
            List<String> t = sshKeys.get(uuid);
            String sshKey = t == null ? null : t.get(0);
            t = rootPasswords.get(uuid);
            String rootPassword = t == null ? null : t.get(0);
            if (sshKey == null && rootPassword == null) {
                continue;
            }

            ret.put(uuid, sshkeyRootPassword(sshKey, rootPassword));
        }

        return ret;
    }
}
