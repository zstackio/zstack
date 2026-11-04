package org.zstack.utils.data;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Names of the entities a JPQL statement refers to, so that a statement can be run by the persistence unit that
 * maps them. Entity names start with an upper case letter; join paths like {@code join t.x y} are not entities.
 */
public class JpqlEntityNames {
    // a from clause ends at the next clause keyword, the end of a sub query or the statement
    private static final Pattern FROM = Pattern.compile(
            "(?i)\\bfrom\\s+(.+?)(?=\\b(?:where|join|inner|left|right|outer|cross|group|order|having|union)\\b|\\)|$)",
            Pattern.DOTALL);
    private static final Pattern UPDATE_OR_JOIN = Pattern.compile(
            "(?i)\\b(?:update|join)\\s+(?:fetch\\s+)?([A-Za-z_][A-Za-z0-9_.]*)");

    public static Set<String> of(String jpql) {
        Set<String> ret = new LinkedHashSet<>();
        Matcher m = FROM.matcher(jpql);
        while (m.find()) {
            for (String item : m.group(1).split(",")) {
                String[] tokens = item.trim().split("\\s+");
                add(tokens[0], ret);
            }
        }
        m = UPDATE_OR_JOIN.matcher(jpql);
        while (m.find()) {
            add(m.group(1), ret);
        }
        return ret;
    }

    private static void add(String name, Set<String> ret) {
        if (!name.isEmpty() && !name.contains(".") && Character.isUpperCase(name.charAt(0))) {
            ret.add(name);
        }
    }
}
