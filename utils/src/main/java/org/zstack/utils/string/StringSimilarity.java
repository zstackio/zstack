package org.zstack.utils.string;

import info.debatty.java.stringsimilarity.*;
import org.apache.commons.io.FileUtils;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by mingjian.deng on 2018/11/28.
 */
public class StringSimilarity {
    private static final CLogger logger = Utils.getLogger(StringSimilarity.class);

    public static final String elaborateFolder = "errorElaborations";
    public static File classPathFolder = PathUtil.findFolderOnClassPath(StringSimilarity.elaborateFolder);
    private static final double threshold = 0.1;
    private static final int mapLength = 1500;

    // matched errors
    private static Map<String, ErrorCodeElaboration> errors = new LinkedHashMap<String, ErrorCodeElaboration>(mapLength, 0.9f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            if (this.size() > mapLength) {
                return true;
            }
            return false;
        }
    };

    // initial errors from json files
    private static List<ErrorCodeElaboration> elaborations = initialElaborations();

    public static boolean matched(ErrorCodeElaboration err) {
        if (err == null) {
            return false;
        }
        if (ElaborationSearchMethod.regex == err.getMethod()) {
            return true;
        }
        if (err.getDistance() > threshold) {
            return false;
        }
        return true;
    }

    public static List<ErrorCodeElaboration> getElaborations() {
        return new ArrayList<>(elaborations);
    }

    public static void resetCachedErrors() {
        errors.clear();
    }

    public static void refreshErrorTemplates() {
        elaborations = initialElaborations();
    }

    private static void validate(List<ErrorCodeElaboration> els) {
        for (ErrorCodeElaboration e: els) {
            DebugUtils.Assert(e.getCategory() != null, String.format("category is null for elaboration: %s", e.getRegex()));
            DebugUtils.Assert(e.getMessage_cn() != null, String.format("message_cn is null for elaboration: %s", e.getRegex()));
        }
    }

    private static List<ErrorCodeElaboration> initialElaborations() {
        List<String> errorTemplates = PathUtil.scanFolderOnClassPath(elaborateFolder);
        List<ErrorCodeElaboration> els = Collections.synchronizedList(new ArrayList<>());
        logger.info("start initializing elaborations");

        for (String errorTemplate: errorTemplates) {
            String name = PathUtil.fileName(errorTemplate);
            if (!name.endsWith(".json")) {
                logger.warn(String.format("found non json file [%s] in path: %s", name, errorTemplate));
                continue;
            }
            File templateFile = new File(errorTemplate);
            try {
                String content = FileUtils.readFileToString(templateFile);
                if (content == null || content.isEmpty()) {
                    continue;
                }
                els.addAll(JSONObjectUtil.toCollection(
                        content,
                        ArrayList.class,
                        ErrorCodeElaboration.class
                ));
                Collections.sort(els, new Comparator<ErrorCodeElaboration>() {
                    @Override
                    public int compare(ErrorCodeElaboration o1, ErrorCodeElaboration o2) {
                        return Integer.valueOf(o1.getCode()) - Integer.valueOf(o2.getCode());
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(String.format("read error elaboration template files failed, due to: %s", e.getMessage()));
            }
        }

        validate(els);
        logger.info(String.format("finish initializing system elaborations, got %s elaborations", els.size()));
        return els;
    }

    public static boolean regexContained(String regex) {
        for (ErrorCodeElaboration e: elaborations) {
            if (e.getRegex().equals(regex)) {
                return true;
            }
        }
        return false;
    }

    public static boolean errorCodeContained(String code) {
        for (ErrorCodeElaboration e: elaborations) {
            String errCode = e.getCategory() + "." + e.getCode();
            if (errCode.equals(code)) {
                return true;
            }
        }
        return false;
    }

    private static double levenshtein(String str, String sub) {
        Levenshtein l = new Levenshtein();
        return l.distance(str, sub);
    }

    private static double normalized(String str, String sub) {
        NormalizedLevenshtein l = new NormalizedLevenshtein();
        return l.distance(str, sub);
    }

    private static double weight(String str, String sub) {
        WeightedLevenshtein l = new WeightedLevenshtein((c, c1) -> 1.0);
        return l.distance(str, sub);
    }

    private static double damerau(String str, String sub) {
        Damerau l = new Damerau();
        return l.distance(str, sub);
    }

    private static double optimal(String str, String sub) {
        OptimalStringAlignment l = new OptimalStringAlignment();
        return l.distance(str, sub);
    }

    private static double jaroWinkler(String str, String sub) {
        JaroWinkler l = new JaroWinkler();
        return l.distance(str, sub);
    }

    private static double longest(String str, String sub) {
        LongestCommonSubsequence l = new LongestCommonSubsequence();
        return l.length(str, sub);
    }

    private static double ngram(String str, String sub) {
        NGram l = new NGram(2);
        return l.distance(str, sub);
    }

    private static double metrics(String str, String sub) {
        MetricLCS l = new MetricLCS();
        return l.distance(str, sub);
    }

    //TODO: could be extensions here
    private static String formatSrc(String str) {
        return str;
    }

    private static List<String> redundanceStrs = CollectionDSL.list("unhandled exception happened when calling");

    private static boolean isRedundance(String sub) {
        for (String redundanceStr: redundanceStrs) {
            if (sub.startsWith(redundanceStr)) {
                return true;
            }
        }
        return false;
    }

    public static ErrorCodeElaboration findSimilary(String sub, Object...args) {
        if (sub == null || sub.isEmpty() || isRedundance(sub)) {
            return null;
        }
        String formatSub = formatSrc(sub);
        if (errors.get(formatSub) != null) {
            return errors.get(formatSub);
        }

        ErrorCodeElaboration err = null;
        long start = System.currentTimeMillis();
        try {
            err = findSimilaryRegex(String.format(formatSub, args));
        } catch (Exception e) {
            logger.warn("exception happened when format error message");
            logger.warn(e.getMessage());
        }

        if (err == null) {
            err = findSimilaryDistance(formatSub);
        }

        if (err != null) {
            err.setFormatSrcError(formatSub);
            errors.put(formatSub, err);
        }

        long end = System.currentTimeMillis();
        logger.debug(String.format("spend %s ms to search elaboration \"%s\"", end - start,
                sub.length() > 50 ? sub.substring(0 , 50) + "..." : sub));

        return err;
    }

    private static ErrorCodeElaboration findSimilaryRegex(String sub) {
        for (ErrorCodeElaboration elaboration: elaborations) {
            if (ElaborationSearchMethod.distance == elaboration.getMethod()) {
                continue;
            }
            if (isRegexMatched(elaboration.getRegex(), sub)) {
                return elaboration;
            }
        }

        return null;
    }

    private static ErrorCodeElaboration findSimilaryDistance(String sub) {
        ErrorCodeElaboration result = null;
        for (ErrorCodeElaboration elaboration: elaborations) {
            if (ElaborationSearchMethod.regex == elaboration.getMethod()) {
                continue;
            }
            double distance = getSimilary(elaboration.getRegex(), sub);
            if (result == null) {
                result = new ErrorCodeElaboration(elaboration);
                result.setDistance(distance);
            } else {
                if (distance < result.getDistance()) {
                    result = new ErrorCodeElaboration(elaboration);
                    result.setDistance(distance);
                }
            }
        }

        return result;
    }

    public static String formatElaboration(ErrorCodeElaboration elaboration, Object...args) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(String.format("错误信息: %s\n", elaboration.getMessage_cn()));

        if (elaboration.getCauses_cn() != null) {
            buffer.append(String.format("可能原因: %s\n", elaboration.getCauses_cn()));
        }

        if (elaboration.getOperation_cn() != null) {
            buffer.append(String.format("操作建议: %s\n", elaboration.getOperation_cn()));
        }

        if (elaboration.getUrl() != null) {
            buffer.append(String.format("更多信息: %s\n", elaboration.getUrl()));
        }

        buffer.deleteCharAt(buffer.lastIndexOf("\n"));
        return String.format(buffer.toString(), args);
    }

    private static boolean isRegexMatched(String regex, String sub) {
        return Pattern.matches(regex, sub);
    }

    private static double getSimilary(String str, String sub) {
        return getSimilary(str, sub, "jaroWinkler");
    }

    private static double getLength(String str, String sub) {
        return getSimilary(str, sub, "optimal");
    }

    private static double getSimilary(String str, String sub, String algorithm) {
        switch (algorithm) {
            case "levenshtein": return levenshtein(str, sub);
            case "normalized": return normalized(str, sub);
            case "weight": return weight(str, sub);
            case "damerau": return damerau(str, sub);
            case "optimal": return optimal(str, sub);
            case "jaroWinkler": return jaroWinkler(str, sub);
            case "longest": return longest(str, sub);
            case "ngram": return ngram(str, sub);
            case "metrics": return metrics(str, sub);
            default: throw new RuntimeException(String.format("error algorithm: %s", algorithm));
        }
    }
}
