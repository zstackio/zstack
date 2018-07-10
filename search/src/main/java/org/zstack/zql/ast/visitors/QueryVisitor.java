package org.zstack.zql.ast.visitors;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.core.StaticInit;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zql.ZQLContext;
import org.zstack.zql.ast.ZQLMetadata;
import org.zstack.zql.ast.visitors.plugin.QueryVisitorPlugin;
import org.zstack.zql.ast.visitors.result.QueryResult;
import org.zstack.zql.ast.visitors.result.ReturnWithResult;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QueryVisitor implements ASTVisitor<QueryResult, ASTNode.Query> {
    private static final CLogger logger = Utils.getLogger(QueryVisitor.class);

    private static Map<QueryVisitorPlugin.ClauseType, Class> plugins = new HashMap<>();

    @StaticInit
    static void staticInit() {
        BeanUtils.reflections.getSubTypesOf(QueryVisitorPlugin.class).stream().filter(clz->!Modifier.isAbstract(clz.getModifiers())).forEach(clz -> {
            try {
                QueryVisitorPlugin p = clz.getConstructor().newInstance();
                plugins.put(p.getClauseType(), clz);
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }
        });
    }

    private QueryVisitorPlugin getPlugin(QueryVisitorPlugin.ClauseType type, ASTNode.Query node) {
        Class clz = plugins.get(type);
        if (clz == null) {
            throw new CloudRuntimeException(String.format("cannot find plugin for ClauseType[%s]", type));
        }

        try {
            return (QueryVisitorPlugin) clz.getConstructor(ASTNode.Query.class).newInstance(node);
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

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

    private SQLText makeSQL(ASTNode.Query node, QueryVisitorPlugin.ClauseType ctype) {
        QueryVisitorPlugin plugin = getPlugin(ctype, node);

        SQLText st = new SQLText();

        ZQLMetadata.InventoryMetadata inventory = ZQLMetadata.findInventoryMetadata(node.getTarget().getEntity());
        ret.inventoryMetadata = inventory;
        ZQLContext.pushQueryTargetInventoryName(inventory.fullInventoryName());

        ret.targetFieldNames = plugin.targetFields();
        if (ret.targetFieldNames == null) {
            ret.targetFieldNames = new ArrayList<>();
        }

        List<String> sqlClauses = new ArrayList<>();
        sqlClauses.add(String.format("SELECT %s FROM", plugin.selectTarget()));
        sqlClauses.add(plugin.tableName());

        List<String> where = new ArrayList<>();
        String conditionWhere = plugin.conditions();
        if (conditionWhere != null && !conditionWhere.isEmpty()) {
            where.add(conditionWhere);
        }
        String restrictByWhere = plugin.restrictBy();
        if (restrictByWhere != null && !restrictByWhere.isEmpty()) {
            where.add(restrictByWhere);
        }

        if (!where.isEmpty()) {
            sqlClauses.add("WHERE");
            sqlClauses.add(StringUtils.join(where, "AND"));
        }

        String groupBy = plugin.groupBy();
        if (groupBy != null) {
            sqlClauses.add(groupBy);
        }

        String orderBy = plugin.orderBy();
        if (orderBy != null) {
            sqlClauses.add(orderBy);
        }

        List<String> jpqlClauses = new ArrayList<>(sqlClauses);

        Integer limit = plugin.limit();
        if (limit != null) {
            sqlClauses.add(String.format("LIMIT %s", limit));
            st.limit = limit;
        }

        Integer offset = plugin.offset();
        if (offset != null) {
            sqlClauses.add(String.format("OFFSET %s", offset));
            st.offset = offset;
        }

        ZQLContext.popQueryTargetInventoryName();

        st.sql = StringUtils.join(sqlClauses, " ");
        st.jpql = StringUtils.join(jpqlClauses, " ");
        return st;
    }

    public QueryResult visit(ASTNode.Query node) {
        SQLText st = makeSQL(node, node instanceof ASTNode.Sum ? QueryVisitorPlugin.ClauseType.SUM : QueryVisitorPlugin.ClauseType.QUERY);
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
                SQLText cst = makeSQL(node, QueryVisitorPlugin.ClauseType.COUNT);
                return emgr.createQuery(cst.jpql);
            };
        }

        if (ret.returnWith != null) {
            // total has handled above
            ret.returnWith.removeIf(it -> it.name.equals("total"));
        }

        return ret;
    }
}
