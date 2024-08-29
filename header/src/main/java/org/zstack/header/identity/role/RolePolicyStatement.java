package org.zstack.header.identity.role;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.zstack.header.identity.AccountConstant.POLICY_BASE_PACKAGE;

public class RolePolicyStatement {
    public RolePolicyEffect effect = RolePolicyEffect.Allow;
    public List<String> actions = new ArrayList<>();
    public List<Resource> resources = new ArrayList<>();

    public static class Resource {
        public String uuid;
        public RolePolicyResourceEffect effect;
        public String resourceType;
        public Resource(String uuid, RolePolicyResourceEffect effect) {
            this.uuid = uuid;
            this.effect = effect;
        }
    }

    public static RolePolicyStatement valueOf(RolePolicyVO policy) {
        String text = toStringStatement(policy);
        return valueOf(text);
    }

    public static RolePolicyStatement valueOf(String policy) {
        String statement = policy.trim();

        RolePolicyStatement value = new RolePolicyStatement();
        if (statement.startsWith("Allow:")) {
            value.effect = RolePolicyEffect.Allow;
            statement = statement.substring("Allow:".length()).trim();
        } else if (statement.startsWith("Exclude:")) {
            value.effect = RolePolicyEffect.Exclude;
            statement = statement.substring("Exclude:".length()).trim();
        }

        final String[] split = statement.split("->");
        if (split.length == 2) {
            value.resources.addAll(parseResource(split[1]));
        } else if (split.length > 2) {
            return null;
        }

        value.actions = Arrays.asList(parseAction(statement));
        return value;
    }

    public static RolePolicyStatement valueOf(Map<String, Object> map) {
        RolePolicyStatement value = new RolePolicyStatement();

        final Object effect = map.get("effect");
        if (effect == null || "Allow".equals(effect)) {
            value.effect = RolePolicyEffect.Allow;
        } else if ("Exclude".equals(effect)) {
            value.effect = RolePolicyEffect.Exclude;
        } else {
            return null;
        }

        final Object resources = map.get("resources");
        if (resources == null) {
            // do-nothing
        } else if (resources instanceof String) {
            value.resources.addAll(parseResource((String) resources));
        } else if (resources instanceof List) {
            ((List<?>) resources).forEach(r -> value.resources.addAll(parseResource((String) r)));
        } else {
            return null;
        }

        final Object actions = map.get("actions");
        if (actions instanceof String) {
            value.actions.add(parseAction((String) actions));
        } else if (actions instanceof List) {
            ((List<?>) actions).forEach(r -> value.actions.add(parseAction(Objects.toString(r))));
        } else {
            return null;
        }

        return value;
    }

    public List<String> toStringStatements() {
        return CollectionUtils.transform(toVO(), RolePolicyStatement::toStringStatement);
    }

    public static String toStringStatement(RolePolicyVO vo) {
        return toStringStatement(vo.getEffect(), vo.getActions());
    }

    public static String toStringStatement(RolePolicyInventory inventory) {
        return toStringStatement(
                RolePolicyEffect.valueOf(inventory.getEffect()), inventory.getActions());
    }

    public static String toStringStatement(RolePolicyEffect effect, String action) {
        String prefix = RolePolicyEffect.Allow.equals(effect) ? "" : effect + ": ";
        return String.format("%s%s", prefix, action);
    }

    public List<RolePolicyVO> toVO() {
        if (resources.isEmpty()) {
            return CollectionUtils.transform(actions, action -> {
                RolePolicyVO vo = new RolePolicyVO();
                vo.setActions(action);
                vo.setEffect(effect);
                return vo;
            });
        }

        throw new CloudRuntimeException("policy with resource is not support now");
    }

    private static List<Resource> parseResource(String statement) {
        final String[] split = statement.trim().split(",");
        return Arrays.stream(split)
                .map(text -> new Resource(text.trim(), RolePolicyResourceEffect.Allow))
                .collect(Collectors.toList());
    }

    public static String parseAction(String statement) {
        statement = statement.trim();

        if (statement.startsWith(POLICY_BASE_PACKAGE)) {
            statement = "." + statement.substring(POLICY_BASE_PACKAGE.length());
        }

        return statement;
    }
}
