package org.zstack.utils.string;

import com.github.sisyphsu.retree.ReMatcher;
import com.github.sisyphsu.retree.ReTree;
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
import java.util.concurrent.ConcurrentHashMap;
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
    public static int maxElaborationRegex = 8192;

    // matched errors
    private static final Map<String, ErrorCodeElaboration> errors = new LinkedHashMap<String, ErrorCodeElaboration>(mapLength, 0.9f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return this.size() > mapLength;
        }
    };

    // missed errors
    private static final Map<String, Boolean> missed = new LinkedHashMap<String, Boolean>(mapLength, 0.9f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return this.size() > mapLength;
        }
    };

    private static final Map<String, Pattern> patterns = new ConcurrentHashMap<>();

    private static ReTree retrees;

    // initial errors from json files
    private static List<ErrorCodeElaboration> elaborations = initialElaborations();

    public static boolean matched(ErrorCodeElaboration err) {
        if (err == null) {
            return false;
        }
        if (ElaborationSearchMethod.regex == err.getMethod()) {
            return true;
        }
        return !(err.getDistance() > threshold);
    }

    public static List<ErrorCodeElaboration> getElaborations() {
        return new ArrayList<>(elaborations);
    }

    public static void resetCachedErrors() {
        synchronized(errors) {
            errors.clear();
        }
        synchronized(missed) {
            missed.clear();
        }
    }

    public static void addErrors(String key, ErrorCodeElaboration err) {
        if (err != null) {
            synchronized(errors) {
                errors.put(key, err);
            }
        }
    }

    public static void addMissed(String key) {
        synchronized(missed){
            missed.put(key, true);
        }
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

    private static void retress(List<ErrorCodeElaboration> els) {
        if (retrees != null) {
            retrees = null;
        }
        List<String> r = new ArrayList<>();
        for (ErrorCodeElaboration e: els) {
            if (ElaborationSearchMethod.distance == e.getMethod()) {
                continue;
            }
            r.add(e.getRegex());
        }
        retrees = new ReTree(r.toArray(new String[0]));
    }

    private static void pattern(List<ErrorCodeElaboration> els) {
        patterns.clear();
        for (ErrorCodeElaboration e: els) {
            if (ElaborationSearchMethod.distance == e.getMethod()) {
                continue;
            }
            patterns.put(e.getRegex(), Pattern.compile(e.getRegex(), Pattern.DOTALL | Pattern.CASE_INSENSITIVE));
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
                els.sort(new Comparator<ErrorCodeElaboration>() {
                    @Override
                    public int compare(ErrorCodeElaboration o1, ErrorCodeElaboration o2) {
                        return Integer.parseInt(o1.getCode()) - Integer.parseInt(o2.getCode());
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(String.format("read error elaboration template files failed, due to: %s", e.getMessage()));
            }
        }

        validate(els);
        pattern(els);
        retress(els);
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

    private static final List<String> redundantStrs = CollectionDSL.list("unhandled exception happened when calling");

    private static boolean isRedundant(String sub) {
        for (String redundantStr: redundantStrs) {
            if (sub.startsWith(redundantStr)) {
                return true;
            }
        }
        return false;
    }

    private static void logSearchSpend(String sub, long start, boolean found) {
        logger.debug(String.format("[%s] spend %s ms to search elaboration \"%s\"", found, System.currentTimeMillis() - start,
                sub.length() > 50 ? sub.substring(0 , 50) + "..." : sub));
    }

    public static ErrorCodeElaboration findSimilar(String sub, Object...args) {
        if (sub == null || sub.isEmpty() || isRedundant(sub)) {
            return null;
        }

        long start = System.currentTimeMillis();
        if (errors.get(sub) != null) {
            logSearchSpend(sub, start, true);
            return errors.get(sub);
        }

        if (missed.get(sub) != null) {
            logSearchSpend(sub, start, false);
            return null;
        }

        ErrorCodeElaboration err;
        try {
            logger.trace(String.format("start to search elaboration for: %s", String.format(sub, args)));
            err = findMostSimilarRegex(String.format(sub, args));
        } catch (Exception e) {
            logger.trace(String.format("start search elaboration for: %s", sub));
            err = findMostSimilarRegex(sub);
        }

        if (err == null) {
            err = findSimilarDistance(sub);
        }

        logSearchSpend(sub, start, err != null);

        return err;
    }

    // better precision, worse performance
    private static ErrorCodeElaboration findMostSimilarRegex(String sub) {
        if (!isRegexMatchedByRetrees(sub)) {
            return null;
        }
        ErrorCodeElaboration matchE = null;
        for (ErrorCodeElaboration elaboration: elaborations) {
            if (ElaborationSearchMethod.distance == elaboration.getMethod()) {
                continue;
            }
            if (isRegexMatched(elaboration.getRegex(), sub)) {
                if (matchE == null) {
                    matchE = elaboration;
                } else if (elaboration.getRegex().length() > matchE.getRegex().length()) {
                    matchE = elaboration;
                }
            }
        }

        return matchE;
    }

    private static ErrorCodeElaboration findSimilarDistance(String sub) {
        ErrorCodeElaboration result = null;
        for (ErrorCodeElaboration elaboration: elaborations) {
            if (ElaborationSearchMethod.regex == elaboration.getMethod()) {
                continue;
            }
            double distance = getSimilar(elaboration.getRegex(), sub);
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

    public static String formatElaboration(String message, Object...args) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(message);

        buffer.deleteCharAt(buffer.lastIndexOf("\n"));
        return String.format(buffer.toString(), args);
    }

    private static boolean isRegexMatchedByRetrees(String sub) {
        return new ReMatcher(retrees, sub).find();
    }

    public static boolean isRegexMatched(String regex, String sub) {
        if (patterns.get(regex) == null) {
            return false;
        }
        return patterns.get(regex).matcher(sub).find();
    }

    private static double getSimilar(String str, String sub) {
        return getSimilar(str, sub, "jaroWinkler");
    }

    private static double getLength(String str, String sub) {
        return getSimilar(str, sub, "optimal");
    }

    private static double getSimilar(String str, String sub, String algorithm) {
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
