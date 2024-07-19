package org.zstack.zql.ast.visitors.plugin;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.zql.ASTNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.zql.ast.visitors.constants.MySqlKeyword.isDistinct;

public class QueryPlugin extends AbstractQueryVisitorPlugin {
    public QueryPlugin() {
    }

    public QueryPlugin(ASTNode.Query node) {
        super(node);
    }

    @Override
    public String selectTarget() {
        String queryTarget = "";
        String groupByTarget = "";
        List<String> queryNames = node.getTarget().getFields() == null ? new ArrayList<>() : node.getTarget().getFields();
        List<String> groupByNames = node.getGroupBy() == null ? new ArrayList<>() : node.getGroupBy().getFields();
        ASTNode.Function function = node.getTarget().getFunction();


        if (queryNames.isEmpty()) {
            if (function != null) {
                if (isDistinct(function)) {
                    queryTarget = String.format("%s %s", function.getFunctionName(), entityAlias);
                } else {
                    queryTarget = String.format("%s(%s)", function.getFunctionName(), entityAlias);
                }
            } else {
                queryTarget = entityAlias;
            }
        }
        if (!queryNames.isEmpty()) {
            if (function != null) {
                if (isDistinct(function)) {
                    queryTarget = String.format("%s %s.%s", function.getFunctionName(), entityAlias, StringUtils.join(queryNames, ","));
                } else {
                    queryTarget = queryNames.stream().map(qt -> String.format("%s(%s.%s)", function.getFunctionName(), entityAlias, qt)).collect(Collectors.joining(","));
                }
            } else {
                queryTarget = StringUtils.join(queryNames, ",");
            }
        }
        if (!groupByNames.isEmpty()) {
            List<String> qt = groupByNames
                    .stream().map(f -> String.format("%s.%s", inventory.simpleInventoryName(), f))
                    .collect(Collectors.toList());
            groupByTarget = StringUtils.join(qt, ",");
        }

        if (StringUtils.isBlank(groupByTarget)) {
            return queryTarget;
        } else {
            if (StringUtils.isNotBlank(queryTarget)) {
                if (isDistinct(function)) {
                    return queryTarget.concat(",").concat(groupByTarget);
                } else {
                    return groupByTarget.concat(",").concat(queryTarget);
                }
            } else {
                return groupByTarget;
            }
        }
    }


    @Override
    public ClauseType getClauseType() {
        return ClauseType.QUERY;
    }
}
