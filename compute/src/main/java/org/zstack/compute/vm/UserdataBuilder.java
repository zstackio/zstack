package org.zstack.compute.vm;

import org.apache.commons.codec.digest.Sha2Crypt;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xing5 on 2016/4/22.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class UserdataBuilder {
    private static final String CRYPT_SALT_CHARS = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final Pattern CRYPT_HASH_PATTERN = Pattern.compile("^\\$(1|2a|2y|2b|5|6|y|gy)(\\$.+){2,}$");
    private static final Pattern CHPASSWD_HEADER_PATTERN = Pattern.compile("^(\\s*)chpasswd\\s*:\\s*(?:#.*)?$");
    private static final Pattern LIST_BLOCK_HEADER_PATTERN = Pattern.compile("^(\\s*)list\\s*:\\s*([|>][-+]?)?\\s*(?:#.*)?$");
    private static final Pattern LIST_FLOW_SEQ_PATTERN = Pattern.compile("^(\\s*list\\s*:\\s*)(\\[.*)$");
    private static final Pattern LIST_INLINE_ENTRY_PATTERN = Pattern.compile("^(\\s*list\\s*:\\s+)([^\\[|>\\s#].*)$");
    private static final Pattern LIST_ENTRY_LINE_PATTERN = Pattern.compile("^(\\s*(?:-\\s+)?)(.+)$");

    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    protected VmInstanceExtensionPointEmitter extEmitter;

    static String hashPassword(String vmUuid, String password) {
        if (password == null || password.isEmpty()
                || "RANDOM".equals(password) || "R".equals(password)
                || CRYPT_HASH_PATTERN.matcher(password).matches()) {
            return password;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String seed = (vmUuid == null ? "" : vmUuid) + ":" + password;
            byte[] hash = digest.digest(seed.getBytes(StandardCharsets.UTF_8));
            StringBuilder salt = new StringBuilder(16);
            for (int i = 0; i < 16; i++) {
                salt.append(CRYPT_SALT_CHARS.charAt(hash[i] & 0x3F));
            }
            return Sha2Crypt.sha512Crypt(password.getBytes(StandardCharsets.UTF_8), "$6$" + salt);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String encryptUserdataPassword(String vmUuid, String userdata) {
        if (userdata == null || !userdata.contains("chpasswd")) {
            return userdata;
        }

        StringBuilder result = new StringBuilder(userdata.length() + 128);
        int chpasswdIndent = -1;
        int listIndent = -1;
        boolean listBlockScalar = false;
        int idx = 0;
        int len = userdata.length();

        while (idx < len) {
            int lineEnd = userdata.indexOf('\n', idx);
            String rawLine = lineEnd == -1 ? userdata.substring(idx) : userdata.substring(idx, lineEnd);
            String lineEnding = "";
            String line = rawLine;
            if (line.endsWith("\r")) {
                line = line.substring(0, line.length() - 1);
                lineEnding = lineEnd == -1 ? "\r" : "\r\n";
            } else if (lineEnd != -1) {
                lineEnding = "\n";
            }

            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                int indent = 0;
                while (indent < line.length() && Character.isWhitespace(line.charAt(indent))) {
                    indent++;
                }

                if (chpasswdIndent >= 0 && indent <= chpasswdIndent) {
                    chpasswdIndent = -1;
                    listIndent = -1;
                    listBlockScalar = false;
                }

                if (chpasswdIndent < 0) {
                    Matcher chpasswdMatcher = CHPASSWD_HEADER_PATTERN.matcher(line);
                    if (chpasswdMatcher.matches()) {
                        chpasswdIndent = chpasswdMatcher.group(1).length();
                    }
                } else {
                    boolean isSameIndentListItem = !listBlockScalar && indent == listIndent && trimmed.startsWith("-");
                    if (listIndent >= 0 && indent <= listIndent && !isSameIndentListItem) {
                        listIndent = -1;
                        listBlockScalar = false;
                    }

                    if (listIndent >= 0) {
                        if (listBlockScalar) {
                            line = rewriteUserPasswordItem(vmUuid, line, true);
                        } else {
                            Matcher entryMatcher = LIST_ENTRY_LINE_PATTERN.matcher(line);
                            if (entryMatcher.matches()) {
                                line = entryMatcher.group(1) + rewriteUserPasswordItem(vmUuid, entryMatcher.group(2), false);
                            }
                        }
                    } else {
                        Matcher listBlockMatcher = LIST_BLOCK_HEADER_PATTERN.matcher(line);
                        if (listBlockMatcher.matches()) {
                            listIndent = listBlockMatcher.group(1).length();
                            listBlockScalar = listBlockMatcher.group(2) != null;
                        } else {
                            Matcher flowSeqMatcher = LIST_FLOW_SEQ_PATTERN.matcher(line);
                            if (flowSeqMatcher.matches()) {
                                line = rewriteInlineFlowSequence(vmUuid, flowSeqMatcher.group(1), flowSeqMatcher.group(2));
                            } else {
                                Matcher inlineMatcher = LIST_INLINE_ENTRY_PATTERN.matcher(line);
                                if (inlineMatcher.matches()) {
                                    line = inlineMatcher.group(1) + rewriteUserPasswordItem(vmUuid, inlineMatcher.group(2), false);
                                }
                            }
                        }
                    }
                }
            }

            result.append(line).append(lineEnding);
            if (lineEnd == -1) {
                break;
            }
            idx = lineEnd + 1;
        }

        return result.toString();
    }

    private static String rewriteInlineFlowSequence(String vmUuid, String prefix, String flowPart) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int closeBracketIdx = -1;
        List<Integer> commaIndices = new ArrayList<>();
        for (int i = 1; i < flowPart.length(); i++) {
            char c = flowPart.charAt(i);
            if (c == '\\' && inDoubleQuote && i + 1 < flowPart.length()) {
                i++;
                continue;
            }
            if (c == '\'' && inSingleQuote && i + 1 < flowPart.length() && flowPart.charAt(i + 1) == '\'') {
                i++;
                continue;
            }
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (!inSingleQuote && !inDoubleQuote) {
                if (c == ',') {
                    commaIndices.add(i);
                } else if (c == ']') {
                    closeBracketIdx = i;
                    break;
                }
            }
        }
        if (closeBracketIdx < 0) {
            return prefix + flowPart;
        }
        StringBuilder sb = new StringBuilder(flowPart.length() + 128);
        sb.append(prefix).append('[');
        int itemStart = 1;
        for (int i = 0; i <= commaIndices.size(); i++) {
            int itemEnd = (i < commaIndices.size()) ? commaIndices.get(i) : closeBracketIdx;
            String rawItem = flowPart.substring(itemStart, itemEnd);
            sb.append(rewriteUserPasswordItem(vmUuid, rawItem, false));
            if (i < commaIndices.size()) {
                sb.append(',');
            }
            itemStart = itemEnd + 1;
        }
        sb.append(flowPart.substring(closeBracketIdx));
        return sb.toString();
    }

    private static String rewriteUserPasswordItem(String vmUuid, String rawItem) {
        return rewriteUserPasswordItem(vmUuid, rawItem, false);
    }

    private static String rewriteUserPasswordItem(String vmUuid, String rawItem, boolean literalBlock) {
        int start = 0;
        int end = rawItem.length();
        while (start < end && Character.isWhitespace(rawItem.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(rawItem.charAt(end - 1))) {
            end--;
        }
        if (start >= end) {
            return rawItem;
        }
        if (!literalBlock) {
            end = stripTrailingYamlComment(rawItem, start, end);
            if (start >= end) {
                return rawItem;
            }
        }
        String token = rawItem.substring(start, end);
        char quote = 0;
        if (!literalBlock && isQuotedScalar(token)) {
            quote = token.charAt(0);
            token = unquoteScalar(token);
        }
        int colonIdx = token.indexOf(':');
        if (colonIdx <= 0) {
            return rawItem;
        }
        String username = token.substring(0, colonIdx).trim();
        if (!literalBlock && quote == 0) {
            username = unquoteScalar(username);
        }
        if (username.isEmpty() || username.indexOf(' ') >= 0 || username.indexOf('\t') >= 0
                || username.indexOf('#') >= 0 || username.indexOf('"') >= 0 || username.indexOf('\'') >= 0) {
            return rawItem;
        }
        String password = token.substring(colonIdx + 1).trim();
        if (!literalBlock && quote == 0) {
            password = unquoteScalar(password);
        }
        if (password.isEmpty()) {
            return rawItem;
        }
        StringBuilder out = new StringBuilder(rawItem.length() + 100);
        out.append(rawItem, 0, start);
        if (quote != 0) {
            out.append(quote);
        }
        out.append(username).append(':').append(hashPassword(vmUuid, password));
        if (quote != 0) {
            out.append(quote);
        }
        out.append(rawItem, end, rawItem.length());
        return out.toString();
    }

    private static int stripTrailingYamlComment(String rawItem, int start, int end) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = start; i < end; i++) {
            char c = rawItem.charAt(i);
            if (inDoubleQuote) {
                if (c == '\\' && i + 1 < end) {
                    i++;
                } else if (c == '"') {
                    inDoubleQuote = false;
                }
            } else if (inSingleQuote) {
                if (c == '\'') {
                    if (i + 1 < end && rawItem.charAt(i + 1) == '\'') {
                        i++;
                    } else {
                        inSingleQuote = false;
                    }
                }
            } else {
                if (c == '"') {
                    inDoubleQuote = true;
                } else if (c == '\'') {
                    inSingleQuote = true;
                } else if (c == '#' && (i == start || Character.isWhitespace(rawItem.charAt(i - 1)))) {
                    end = i;
                    break;
                }
            }
        }
        while (end > start && Character.isWhitespace(rawItem.charAt(end - 1))) {
            end--;
        }
        return end;
    }

    private static boolean isQuotedScalar(String value) {
        if (value == null || value.length() < 2) {
            return false;
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if (first == '"' && last == '"') {
            for (int i = 1; i < value.length() - 1; i++) {
                char c = value.charAt(i);
                if (c == '\\') {
                    i++;
                    if (i >= value.length() - 1) {
                        return false;
                    }
                } else if (c == '"') {
                    return false;
                }
            }
            return true;
        }
        if (first == '\'' && last == '\'') {
            for (int i = 1; i < value.length() - 1; i++) {
                if (value.charAt(i) == '\'') {
                    if (i + 1 < value.length() - 1 && value.charAt(i + 1) == '\'') {
                        i++;
                    } else {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    private static String unquoteScalar(String value) {
        if (!isQuotedScalar(value)) {
            return value;
        }
        char quote = value.charAt(0);
        String inner = value.substring(1, value.length() - 1);
        if (quote == '\'') {
            return inner.replace("''", "'");
        }
        return unescapeYamlDoubleQuoted(inner);
    }

    private static String unescapeYamlDoubleQuoted(String inner) {
        if (inner.indexOf('\\') < 0) {
            return inner;
        }
        StringBuilder sb = new StringBuilder(inner.length());
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c != '\\' || i + 1 >= inner.length()) {
                sb.append(c);
                continue;
            }
            char next = inner.charAt(++i);
            switch (next) {
                case '0':
                    sb.append('\0');
                    break;
                case 'a':
                    sb.append('\u0007');
                    break;
                case 'b':
                    sb.append('\b');
                    break;
                case 't':
                case '\t':
                    sb.append('\t');
                    break;
                case 'n':
                    sb.append('\n');
                    break;
                case 'v':
                    sb.append('\u000b');
                    break;
                case 'f':
                    sb.append('\f');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                case 'e':
                    sb.append('\u001b');
                    break;
                case ' ':
                    sb.append(' ');
                    break;
                case '"':
                    sb.append('"');
                    break;
                case '/':
                    sb.append('/');
                    break;
                case '\\':
                    sb.append('\\');
                    break;
                case 'N':
                    sb.append('\u0085');
                    break;
                case '_':
                    sb.append('\u00a0');
                    break;
                case 'L':
                    sb.append('\u2028');
                    break;
                case 'P':
                    sb.append('\u2029');
                    break;
                case 'x':
                    if (i + 2 < inner.length()) {
                        int cp = parseHex(inner, i + 1, 2);
                        if (cp >= 0) {
                            sb.append((char) cp);
                            i += 2;
                            break;
                        }
                    }
                    sb.append('\\').append(next);
                    break;
                case 'u':
                    if (i + 4 < inner.length()) {
                        int cp = parseHex(inner, i + 1, 4);
                        if (cp >= 0) {
                            sb.append((char) cp);
                            i += 4;
                            break;
                        }
                    }
                    sb.append('\\').append(next);
                    break;
                case 'U':
                    if (i + 8 < inner.length()) {
                        int cp = parseHex(inner, i + 1, 8);
                        if (cp >= 0 && Character.isValidCodePoint(cp)) {
                            sb.append(Character.toChars(cp));
                            i += 8;
                            break;
                        }
                    }
                    sb.append('\\').append(next);
                    break;
                default:
                    sb.append('\\').append(next);
                    break;
            }
        }
        return sb.toString();
    }

    private static int parseHex(String s, int offset, int len) {
        int val = 0;
        for (int i = 0; i < len; i++) {
            int digit = Character.digit(s.charAt(offset + i), 16);
            if (digit < 0) {
                return -1;
            }
            val = (val << 4) | digit;
        }
        return val;
    }

    private String sshkeyRootPassword(String vmUuid, List<String> sshKeys, String rootPassword) {
        StringBuilder sb = new StringBuilder();
        if (!sshKeys.isEmpty()) {
            sb.append("\nssh_authorized_keys:");
            sshKeys.forEach(sshKey -> {
                if (sshKey != null) {
                    sb.append(String.format("\n  - %s", sshKey));
                }
            });
            sb.append("\ndisable_root: false");
        }

        if (rootPassword != null) {
            sb.append("\nchpasswd:");
            sb.append("\n  list: |");
            sb.append(String.format("\n    root:%s", hashPassword(vmUuid, rootPassword)));
            sb.append("\n  expire: False");
        }

        return sb.toString();
    }

    public List<String> buildByVmUuid(String vmUuid) {
        List<String> userdataList = new ArrayList<>();

        String userdata = VmSystemTags.USERDATA.getTokenByResourceUuid(vmUuid, VmSystemTags.USERDATA_TOKEN);
        if (userdata != null) {
            userdata = new String(Base64.getDecoder().decode(userdata.getBytes()));
            userdataList.add(encryptUserdataPassword(vmUuid, userdata));
        }

        List<String> sshKeys = extEmitter.fetchAssociatedSshKeyPairs(vmUuid);

        String sshKey = VmSystemTags.SSHKEY.getTokenByResourceUuid(vmUuid, VmSystemTags.SSHKEY_TOKEN);
        if (sshKey != null) {
            sshKeys.add(sshKey);
        }
        String rootPassword = VmSystemTags.ROOT_PASSWORD.getTokenByResourceUuid(vmUuid, VmSystemTags.ROOT_PASSWORD_TOKEN);
        if (!sshKeys.isEmpty() || rootPassword != null) {
            userdataList.add(String.format("#cloud-config%s", sshkeyRootPassword(vmUuid, sshKeys, rootPassword)));
        }

        return userdataList;
    }

    public Map<String, List<String>> buildByVmUuids(List<String> vmUuids) {
        Map<String, List<String>> ret = new HashMap<>();
        for (String vmUuid : vmUuids) {
            ret.put(vmUuid, buildByVmUuid(vmUuid));
        }
        return ret;
    }
}
