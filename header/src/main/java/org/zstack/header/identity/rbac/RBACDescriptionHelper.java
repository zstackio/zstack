package org.zstack.header.identity.rbac;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RBACDescriptionHelper {
    protected static class FlattenResult {
        Set<String> adminOnly = new HashSet<>();
        Set<String> normal = new HashSet<>();
    }

    private static PolicyMatcher matcher = new PolicyMatcher();

    static FlattenResult flatten(Set<String> adminInput, Set<String> normalInput) {
        return _flatten(adminInput, normalInput);
    }

    private static FlattenResult _flatten(Set<String> adminInput, Set<String> normalInput) {
        boolean is = adminInput.stream().anyMatch(a -> normalInput.stream().anyMatch(n -> matcher.match(a, n) || matcher.match(n, a)));

        FlattenResult ret = new FlattenResult();

        if (is) {
            // declarations of admin APIs and normal APIs have conflict, flatten them to precise declarations
            APIMessage.apiMessageClasses.forEach(apiClz -> {
                Set<String> adminRules = adminInput.stream().filter(it-> matcher.match(it, apiClz.getName())).collect(Collectors.toSet());
                Set<String> normalRules = normalInput.stream().filter(it -> matcher.match(it ,apiClz.getName())).collect(Collectors.toSet());

                if (adminRules.isEmpty() && normalRules.isEmpty()) {
                    return;
                }

                class Winner {
                    String winner;
                }

                final Winner winner = new Winner();

                BiFunction<String, String, String> getWinnerRule = (String r1, String r2) -> {
                    String rule = matcher.returnPrecisePattern(apiClz.getName(), r1, r2);
                    if (rule == null) {
                        throw new CloudRuntimeException(String.format("ambiguous rules: %s and %s both matches the API[%s]", r1, r2, apiClz.getName()));
                    }
                    return rule;
                };

                Stream.concat(adminRules.stream(), normalRules.stream()).forEach(it -> {
                    if (winner.winner == null) {
                        winner.winner = it;
                    } else {
                        winner.winner = getWinnerRule.apply(it, winner.winner);
                    }
                });

                if (winner.winner == null) {
                    throw new CloudRuntimeException(String.format("adminRules:%s, normalRules:%s", adminRules, normalInput));
                }

                if (adminRules.contains(winner.winner)) {
                    ret.adminOnly.add(apiClz.getName());
                } else {
                    ret.normal.add(apiClz.getName());
                }
            });
        } else {
            ret.adminOnly.addAll(adminInput);
            ret.normal.addAll(normalInput);
        }

        return ret;
    }

    static RBAC.Permission flatten(RBAC.Permission permission) {
        FlattenResult fr = _flatten(permission.get_adminOnlyAPIs(), permission.get_normalAPIs());
        permission.setAdminOnlyAPIs(fr.adminOnly);
        permission.setNormalAPIs(fr.normal);
        return permission;
    }
}
