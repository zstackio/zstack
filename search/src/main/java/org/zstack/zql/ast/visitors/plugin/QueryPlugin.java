package org.zstack.zql.ast.visitors.plugin;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.zstack.header.zql.ASTNode;

import java.util.List;

public class QueryPlugin extends AbstractQueryVisitorPlugin {
    public QueryPlugin() {
    }

    public QueryPlugin(ASTNode.Query node) {
        super(node);
    }

    @Override
    public String selectTarget() {
        String queryTarget;
        List<String> fieldNames = targetFields();

        if (fieldNames.isEmpty()) {
            queryTarget = entityAlias;
        } else {
            queryTarget = StringUtils.join(buildFieldsWithoutFunction(), ",");
        }

        String target = String.format(functions(), queryTarget);
        String fieldWithFunction = StringUtils.join(buildFieldsWithFunction(), ",");
        if (Strings.isNotEmpty(fieldWithFunction)) {
            target = target + "," + fieldWithFunction;
        }

        return target;
    }


    @Override
    public ClauseType getClauseType() {
        return ClauseType.QUERY;
    }
}
