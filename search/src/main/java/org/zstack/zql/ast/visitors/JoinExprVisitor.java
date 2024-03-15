package org.zstack.zql.ast.visitors;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.Platform;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;
import org.zstack.utils.CollectionUtils;
import org.zstack.zql.ast.ZQLMetadata;

public class JoinExprVisitor implements ASTVisitor<String, ASTNode.JoinExpr> {
    @Override
    public String visit(ASTNode.JoinExpr node) {
        String left = (String) node.getLeft().accept(columnVisitor());
        String operator = node.getOperator();
        String right = (String) node.getRight().accept(columnVisitor());
        return String.format("%s %s %s", left, operator, right);
    }

    private static ASTVisitor<String, ASTNode.QueryTarget> columnVisitor() {
        return new ASTVisitor<String, ASTNode.QueryTarget>() {
            @Override
            public String visit(ASTNode.QueryTarget queryTarget) {
                if (StringUtils.isBlank(queryTarget.getEntity())) {
                    throw new OperationFailureException(Platform.operr("entity is empty, cannot get columnName"));
                }
                if (CollectionUtils.isEmpty(queryTarget.getFields())) {
                    throw new OperationFailureException(Platform.operr("fieldList is empty, cannot get columnName"));
                }
                String entity = queryTarget.getEntity();
                String field = queryTarget.getFields().get(0);
                String alias = ZQLMetadata.findInventoryMetadata(entity).simpleInventoryName();
                return String.format("%s.%s", alias, field);
            }
        };
    }
}
