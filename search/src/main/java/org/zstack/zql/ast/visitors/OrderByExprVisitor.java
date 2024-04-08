package org.zstack.zql.ast.visitors;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.Platform;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;
import org.zstack.zql.ast.ZQLMetadata;

/**
 * Created by MaJin on 2020/4/29.
 */
public class OrderByExprVisitor implements ASTVisitor<String, ASTNode.OrderByExpr> {
    @Override
    public String visit(ASTNode.OrderByExpr node) {
        if (!"asc".equalsIgnoreCase(node.getDirection()) && !"desc".equalsIgnoreCase(node.getDirection())) {
            throw new ZQLError(Platform.i18n("invalid order by clause, expect direction[asc,desc] but got %s", node.getDirection()));
        }

        ASTNode.ExprAtom expr = node.getExpr();
        String defaultColumn = expr.getText();
        String entityColumn = getEntityColumn(expr);
        String functionCall = expr.getFunction() != null
                ? (String) expr.getFunction().accept(new FunctionVisitor())
                : "";

        String orderColumn;
        if (StringUtils.isNotBlank(functionCall)) {
            if (StringUtils.isNotBlank(defaultColumn)) {
                orderColumn = String.format(functionCall, defaultColumn);
            } else {
                orderColumn = String.format(functionCall, entityColumn);
            }
        } else if (StringUtils.isNotBlank(entityColumn)) {
            orderColumn = entityColumn;
        } else if (StringUtils.isNotBlank(defaultColumn)) {
            orderColumn = defaultColumn;
        } else {
            throw new ZQLError(Platform.i18n("invalid order by clause"));
        }

        return String.format("%s %s", orderColumn, node.getDirection().toUpperCase());
    }

    private String getEntityColumn(ASTNode.ExprAtom expr) {
        if (expr == null || expr.getQueryTarget() == null) return "";
        ASTNode.QueryTarget qt = expr.getQueryTarget();
        String entityVO = ZQLMetadata.findInventoryMetadata(qt.getEntity()).simpleInventoryName();

        return String.format("%s.%s", entityVO, StringUtils.join(qt.getFields(), ","));
    }
}
