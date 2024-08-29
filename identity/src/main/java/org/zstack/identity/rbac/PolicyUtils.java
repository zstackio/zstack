package org.zstack.identity.rbac;

import org.zstack.header.identity.rbac.RBAC;
import org.zstack.header.identity.role.RolePolicyEffect;
import org.zstack.header.identity.role.RolePolicyVO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.zstack.core.Platform.BASE_PACKAGE_NAME;
import static org.zstack.utils.CollectionUtils.filter;
import static org.zstack.utils.CollectionUtils.transformToSet;

public class PolicyUtils {
    public static boolean isAdminOnlyAction(String action) {
        return RBAC.isAdminOnlyAPI(apiNamePatternFromAction(action));
    }

    public static String apiNamePatternFromAction(String action) {
        return apiNamePatternFromAction(action, false);
    }

    public static String apiNamePatternFromAction(String action, boolean oldPolicy) {
        if (!oldPolicy) {
            return action.split(":")[0];
        }

        String[] splited = action.split(":");

        if (splited.length != 2) {
            return splited[0];
        } else {
            return splited[1];
        }
    }

    /**
     * Example:<br/>
     *
     * input: "org.zstack.header.vm.APIStartVmInstanceMsg"<br/>
     * output [".**", ".header.**", ".header.vm.**", ".header.vm.*", ".header.vm.APIStartVmInstanceMsg"]<br/>
     */
    public static List<String> findAllMatchedApiPatterns(String api) {
        List<String> results = new ArrayList<>();

        if (!api.startsWith(BASE_PACKAGE_NAME)) {
            results.add(api);
            return results;
        }

        api = api.substring(BASE_PACKAGE_NAME.length()); // start without "."
        String[] split = api.split("\\.");
        results.add(".**");

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < split.length - 1; i++) {
            builder.append(".").append(split[i]);
            results.add(builder + ".**");
        }

        results.add(builder + ".*");
        results.add("." + api);
        return results;
    }

    /**
     * <p><b>Example 1</b><br/>
     * input: ["API", "Exclude: API"]<br/>
     * output ["Exclude: API"]<br/>
     *
     * <p><b>Example 2</b><br/>
     * input: ["Exclude: API", "API"]<br/>
     * output ["API"]<br/>
     *
     * <p><b>Example 3</b><br/>
     * input: ["Exclude: API", "API", "API", "API", "API", "API"]<br/>
     * output ["API"]<br/>
     *
     * <p><b>Example 4</b><br/>
     * input: ["Exclude: API", "API", "Exclude: API", "API", "Exclude: API"<br/>
     * output ["Exclude: API"] (because last one is Exclude)<br/>
     *
     * <p><b>Example 5</b><br/>
     * input: ["API -> VmInstanceVO", "API -> VolumeVO"<br/>
     * output ["API -> VmInstanceVO,VolumeVO"] ( TODO )<br/>
     *
     * <p><b>Example 6</b><br/>
     * input: ["API -> VmInstanceVO", "API -> VolumeVO", "Exclude: API"<br/>
     * output ["Exclude: API"]<br/>
     *
     * <p><b>Example 6</b><br/>
     * input: ["API -> VmInstanceVO", "Exclude: API", "API -> VolumeVO"<br/>
     * output ["API -> VolumeVO"] (because last one is allow with specific VolumeVO)<br/>
     *
     * </p>
     */
    public static List<RolePolicyVO> formatPolicies(List<RolePolicyVO> list, String roleUuid) {
        Set<String> actions = transformToSet(list, RolePolicyVO::getActions);
        Map<String, RolePolicyVO> groupByResourceTypes = new HashMap<>();
        RolePolicyVO withoutResources = null;

        List<RolePolicyVO> results = new ArrayList<>();
        for (String action : actions) {
            List<RolePolicyVO> relatedPolicies = filter(list, vo -> Objects.equals(vo.getActions(), action));
            if (relatedPolicies.size() == 1) {
                results.add(relatedPolicies.get(0));
                continue;
            }

            groupByResourceTypes.clear();
            withoutResources = null;

            ListIterator<RolePolicyVO> iterator = relatedPolicies.listIterator(relatedPolicies.size());
            for (; iterator.hasPrevious();) {
                RolePolicyVO policy = iterator.previous();

                if (policy.getEffect() == RolePolicyEffect.Exclude) {
                    if (groupByResourceTypes.isEmpty() && withoutResources == null) {
                        withoutResources = policy;
                    }
                    break;
                }

                if (policy.getEffect() == RolePolicyEffect.Allow && policy.getResourceType() == null) {
                    withoutResources = policy;
                    continue;
                }

                // TODO: resource UUID will support soon
//                if (policy.getEffect() == RolePolicyEffect.Allow && policy.getResourceType() != null) {
//                    RolePolicyVO existsPolicy = groupByResourceTypes.get(policy.getResourceType());
//                    if (existsPolicy == null) {
//                        groupByResourceTypes.put(policy.getResourceType(), policy);
//                        continue;
//                    }
//
//                    existsPolicy.getResourceRefs().addAll(policy.getResourceRefs());
//                }
            }

            if (withoutResources == null) {
                results.addAll(groupByResourceTypes.values());
            } else {
                results.add(withoutResources);
            }
        }

        results.forEach(policy -> policy.setRoleUuid(roleUuid));
        return results;
    }
}
