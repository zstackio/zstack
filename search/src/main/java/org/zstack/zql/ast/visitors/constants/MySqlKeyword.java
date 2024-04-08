package org.zstack.zql.ast.visitors.constants;

import java.util.HashMap;

public class MySqlKeyword {
    /**
     * TODO No elegant way has been found to distinguish between 'keywords' and' functions'
     * Because some mysql keywords need () and some do not need, and in order to be compatible with Hibernate HQL.
     * So maintain this mapping table
     */
    public static final HashMap<String, String> keywordMap =new HashMap<>();

    static {
        keywordMap.put("DISTINCT", "DISTINCT %s");
        keywordMap.put("SHA1", "SHA1(%s)");
        // ... Keywords too many (hundreds) found the need to supplement
    }
}
