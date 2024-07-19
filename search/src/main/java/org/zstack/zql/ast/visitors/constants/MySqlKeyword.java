package org.zstack.zql.ast.visitors.constants;

import org.zstack.header.zql.ASTNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MySqlKeyword {
    /**
     * TODO No elegant way has been found to distinguish between 'keywords' and' functions'
     * Because some mysql keywords need () and some do not need, and in order to be compatible with Hibernate HQL.
     * So maintain this mapping table
     */
    public static final HashMap<String, String> keywordMap = new HashMap<>();

    public static final String MAX = "MAX";
    public static final String MIN = "MIN";
    public static final String COUNT = "COUNT";
    public static final String SUM = "SUM";
    public static final String AVG = "AVG";
    public static final String DISTINCT = "DISTINCT";


    static {
        // ... Keywords too many (hundreds) found the need to supplement
        keywordMap.put(DISTINCT, "DISTINCT %s");

    }

    public static boolean isMax(ASTNode.Function function) {
        if (function == null) return false;
        return function.getFunctionName().equalsIgnoreCase(MAX);
    }

    public static boolean isMin(ASTNode.Function function) {
        if (function == null) return false;
        return function.getFunctionName().equalsIgnoreCase(MIN);
    }

    public static boolean isCount(ASTNode.Function function) {
        if (function == null) return false;
        return function.getFunctionName().equalsIgnoreCase(COUNT);
    }

    public static boolean isSum(ASTNode.Function function) {
        if (function == null) return false;
        return function.getFunctionName().equalsIgnoreCase(SUM);
    }

    public static boolean isAvg(ASTNode.Function function) {
        if (function == null) return false;
        return function.getFunctionName().equalsIgnoreCase(AVG);
    }

    public static boolean isDistinct(ASTNode.Function function) {
        if (function == null) return false;
        return function.getFunctionName().equalsIgnoreCase(DISTINCT);
    }
}
