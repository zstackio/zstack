package org.zstack.zql.ast.visitors;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;
import org.zstack.zql.ZQLContext;
import org.zstack.zql.ast.ZQLMetadata;
import org.zstack.zql.ast.visitors.result.QueryResult;
import org.zstack.zql.ast.visitors.result.ReturnWithResult;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class QueryVisitor implements ASTVisitor<QueryResult, ASTNode.Query> {
    QueryResult ret = new QueryResult();

    private boolean countQuery;

    public QueryVisitor(boolean countQuery) {
        this.countQuery = countQuery;
    }

    private String makeConditions(ASTNode.Query node) {
        if (node.getConditions() == null || node.getConditions().isEmpty()) {
            return "";
        }

        List<String> conds = node.getConditions().stream().map(it->(String)((ASTNode)it).accept(new ConditionVisitor())).collect(Collectors.toList());
        return StringUtils.join(conds, " ");
    }

    private class SQLText {
        String sql;
        // JPQL doesn't not support limit and offset clause
        String jpql;
        Integer limit;
        Integer offset;
    }

    private SQLText makeSQL(ASTNode.Query node, boolean countClause) {
        SQLText st = new SQLText();

        ZQLMetadata.InventoryMetadata inventory = ZQLMetadata.findInventoryMetadata(node.getTarget().getEntity());
        ret.inventoryMetadata = inventory;
        ZQLContext.pushQueryTargetInventoryName(inventory.fullInventoryName());

        String fieldName = node.getTarget().getFields() == null || node.getTarget().getFields().isEmpty() ? "" : node.getTarget().getFields().get(0);
        if (!fieldName.equals("")) {
            inventory.errorIfNoField(fieldName);
            ret.targetFieldName = fieldName;
        }

        String entityAlias = inventory.simpleInventoryName();
        String queryTarget = fieldName.equals("") ? entityAlias : String.format("%s.%s", inventory.simpleInventoryName(), fieldName);
        String entityVOName = inventory.inventoryAnnotation.mappingVOClass().getSimpleName();

        List<String> sqlClauses = new ArrayList<>();

        if (countClause) {
            sqlClauses.add(String.format("SELECT count(*) FROM %s %s", entityVOName, entityAlias));
        } else {
            sqlClauses.add(String.format("SELECT %s FROM %s %s", queryTarget, entityVOName, entityAlias));
        }

        String condition = makeConditions(node);
        String restrictBy = node.getRestrictBy() == null ? null : (String) node.getRestrictBy().accept(new RestrictByVisitor());

        if (!condition.equals("") || restrictBy != null) {
            sqlClauses.add("WHERE");
        }

        List<String> conditionClauses = new ArrayList<>();
        if (!condition.equals("")) {
            conditionClauses.add(condition);
        }

        if (restrictBy != null) {
            conditionClauses.add(restrictBy);
        }

        if (!conditionClauses.isEmpty()) {
            sqlClauses.add(StringUtils.join(conditionClauses, " AND "));
        }

        if (node.getOrderBy() != null) {
            sqlClauses.add((String) node.getOrderBy().accept(new OrderByVisitor()));
        }

        List<String> jpqlClauses = new ArrayList<>(sqlClauses);

        if (node.getLimit() != null) {
            LimitVisitor v = new LimitVisitor();
            sqlClauses.add((String) node.getLimit().accept(v));
            assert v.limit != null;
            st.limit = v.limit;
        }

        if (node.getOffset() != null) {
            OffsetVisitor v = new OffsetVisitor();
            sqlClauses.add((String) node.getOffset().accept(v));
            assert v.offset != null;
            st.offset = v.offset;
        }

        ZQLContext.popQueryTargetInventoryName();

        st.sql = StringUtils.join(sqlClauses, " ");
        st.jpql = StringUtils.join(jpqlClauses, " ");
        return st;
    }

    public QueryResult visit(ASTNode.Query node) {
        SQLText st = makeSQL(node, false);
        ret.sql = st.sql;
        ret.createJPAQuery = (EntityManager emgr) -> {
            Query q = emgr.createQuery(st.jpql);
            if (st.limit != null) {
                q.setMaxResults(st.limit);
            }
            if (st.offset != null) {
                q.setFirstResult(st.offset);
            }

            return q;
        };

        if (node.getReturnWith() != null) {
            ret.returnWith = (List<ReturnWithResult>) node.getReturnWith().accept(new ReturnWithVisitor());
        }

        if (countQuery || (ret.returnWith != null && ret.returnWith.stream().anyMatch(it->it.name.equals("total")))) {
            ret.createCountQuery = (EntityManager emgr) -> {
                SQLText cst = makeSQL(node, true);
                return emgr.createQuery(cst.jpql);
            };
        }

        return ret;
    }
}
