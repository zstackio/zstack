package org.zstack.zql.ast.visitors;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.Platform;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;
import org.zstack.zql.ast.ZQLMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JoinClauseVisitor implements ASTVisitor<String, ASTNode.JoinClause> {
    @Override
    public String visit(ASTNode.JoinClause node) {
        if (node.getConditions() == null) {
            throw new OperationFailureException(Platform.operr("join condition is missing"));
        }

        String tableNameAndAlias = (String) node.getQueryTarget().accept(tableNameVisitor());

        List<String> joinTableStrList = new ArrayList<>();
        joinTableStrList.add(node.getJoinType());
        joinTableStrList.add(node.getJoin());
        joinTableStrList.add(tableNameAndAlias);
        joinTableStrList.add(node.getOn());
        List<String> conditions = node.getConditions().stream()
                .map(condition -> {
                    if (condition instanceof ASTNode.JoinExpr) {
                        return (String) ((ASTNode.JoinExpr) condition).accept(new JoinExprVisitor());
                    } else if (condition instanceof ASTNode.LogicalOperator) {
                        return (String) ((ASTNode.LogicalOperator) condition).accept(new LogicalOperatorVisitor());
                    }
                    return "1=1";
                })
                .collect(Collectors.toList());
        joinTableStrList.addAll(conditions);

        return StringUtils.join(joinTableStrList, " ");
    }

    private static ASTVisitor<String, ASTNode.QueryTarget> tableNameVisitor() {
        return new ASTVisitor<String, ASTNode.QueryTarget>() {
            @Override
            public String visit(ASTNode.QueryTarget queryTarget) {
                if (StringUtils.isBlank(queryTarget.getEntity())) {
                    throw new OperationFailureException(Platform.operr("entity is empty, cannot get TableName"));
                }

                String entity = queryTarget.getEntity();
                String tableName = ZQLMetadata.findInventoryMetadata(entity).inventoryAnnotation.mappingVOClass().getSimpleName();
                String alias = ZQLMetadata.findInventoryMetadata(entity).simpleInventoryName();

                return String.format("%s %s", tableName, alias);
            }
        };
    }
}
