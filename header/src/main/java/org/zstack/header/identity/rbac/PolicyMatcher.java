package org.zstack.header.identity.rbac;

import org.springframework.util.AntPathMatcher;

import java.util.Comparator;

public class PolicyMatcher {
    private AntPathMatcher matcher = new AntPathMatcher();

    public boolean match(String policy, String path) {
        policy = policy.replaceAll("\\.", "/");
        path = path.replaceAll("\\.", "/");
        return matcher.match(policy, path);
    }

    public String returnPrecisePattern(String path, String p1, String p2) {
        Comparator<String> c = matcher.getPatternComparator(path);
        int ret = c.compare(p1, p2);
        if (ret < 0) {
            return p1;
        } else if (ret > 0) {
            return p2;
        } else {
            return null;
        }
    }
}
