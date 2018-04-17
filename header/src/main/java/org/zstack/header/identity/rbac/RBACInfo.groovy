package org.zstack.header.identity.rbac

import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

class RBACInfo {
    private static final CLogger logger = Utils.getLogger(RBACInfo.class)

    private Set<String> adminOnlyAPIs = []
    private Set<String> normalAPIs = []
    List<Class> targetResources = []
    private Set<String> _adminOnlyAPIs = []
    private Set<String> _normalAPIs = []
    String name

    boolean isTargetResource(Class clz) {
        return targetResources.any { clz.isAssignableFrom(it) }
    }

    List<String> adminOnlyAPIs(Class...apis) {
        List<String> lst = (apis as Set).collect { it.name }
        _adminOnlyAPIs.addAll(lst)
        return lst
    }

    List<String> adminOnlyAPIs(String...apis) {
        _adminOnlyAPIs.addAll(apis as Set)
        return apis as List
    }

    List<String> normalAPIs(String...apis) {
        _normalAPIs.addAll(apis as Set)
        return apis as List
    }

    List<String> normalAPIs(Class...apis) {
        List<String> lst = (apis as Set).collect { it.name }
        _normalAPIs.addAll(lst)
        return lst
    }

    Set<String> getAdminOnlyAPIs() {
        return adminOnlyAPIs
    }

    Set<String> getNormalAPIs() {
        return normalAPIs
    }
}
