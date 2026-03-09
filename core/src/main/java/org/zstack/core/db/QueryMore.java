package org.zstack.core.db;

import org.apache.commons.collections.CollectionUtils;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.DebugUtils;

import jakarta.persistence.Tuple;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import static org.zstack.core.db.SimpleQuery.Op.*;

public class QueryMore {
    private final Class<?>[] tables;
    private List<AttrInfo> selects = new ArrayList<>();
    private List<OrderInfo> orders = new ArrayList<>();
    private List<Condition> conditions = new ArrayList<>();
    private List<ConditionBetweenTables> moreConditions = new ArrayList<>();
    private Integer limit;
    private Integer start;

    private int tableIndex;
    private int tableOffset; // used in sub-query

    QueryMore(Class<?> one, Class<?>... more) {
        this.tables = new Class<?>[more.length + 1];
        this.tables[0] = one;

        if (more.length > 0) {
            System.arraycopy(more, 0, this.tables, 1, more.length);
        }
    }

    public class AttrInfo {
        SingularAttribute<?, ?> attr;
        String function;
        int tableIndex;

        private String toSql() {
            if (function != null) {
                return attr == null ?
                        String.format("%s(t%d)", function, tableIndex + tableOffset) :
                        String.format("%s(t%d.%s)", function, tableIndex + tableOffset, attr.getName());
            }

            return attr == null ?
                    String.format("t%d", tableIndex + tableOffset) :
                    String.format("t%d.%s", tableIndex + tableOffset, attr.getName());
        }
    }

    public class OrderInfo {
        SingularAttribute<?, ?> attr;
        SimpleQuery.Od od;
        int tableIndex;

        private String toSql() {
            return String.format("t%d.%s %s", tableIndex + tableOffset, attr.getName(), od.name());
        }
    }

    public static class TableOption<ResultType> {
        IntFunction<ResultType> function;
        Consumer<SingularAttribute<?, ?>> attrConsumer;

        public ResultType table0(Object attr) {
            return table(0, attr);
        }

        public ResultType table1(Object attr) {
            return table(1, attr);
        }

        public ResultType table2(Object attr) {
            return table(2, attr);
        }

        public ResultType table3(Object attr) {
            return table(3, attr);
        }

        public ResultType table(int tableIndex, Object attr) {
            attrConsumer.accept((SingularAttribute<?, ?>) attr);
            return function.apply(tableIndex);
        }
    }

    public class Condition {
        SingularAttribute<?, ?> attr;
        SimpleQuery.Op op;
        Object value;
        int tableIndex;

        private String toSql(int paramIndex) {
            switch (op) {
            case NULL: case NOT_NULL:
                return String.format("t%d.%s %s", tableIndex + tableOffset, attr.getName(), op.toString());
            case IN: case NOT_IN: case LIKE: case NOT_LIKE:
                return String.format("t%d.%s %s :p%d", tableIndex + tableOffset, attr.getName(), op.toString(), paramIndex);
            }
            return String.format("t%d.%s%s:p%d", tableIndex + tableOffset, attr.getName(), op.toString(), paramIndex);
        }

        private boolean needParam() {
            return op != NULL && op != NOT_NULL;
        }

        private boolean hasSubQuery() {
            return value instanceof QueryMore;
        }

        private String handleSubQuery(Map<String, Object> params, int tableOffset) {
            if (value instanceof QueryMore) {
                final QueryMore q = (QueryMore) value;
                q.tableOffset = tableOffset;
                final String sql = q.toSqlAndFillParamMap(params);
                return String.format("t%d.%s %s (%s)",
                        tableIndex + QueryMore.this.tableOffset,
                        attr.getName(), op.toString(), sql);
            }
            throw new CloudRuntimeException("should not be here");
        }
    }

    public class ConditionBetweenTables {
        int tableIndex1;
        SingularAttribute<?, ?> attr1;
        SimpleQuery.Op op;
        int tableIndex2;
        SingularAttribute<?, ?> attr2;

        private String toSql() {
            return String.format("t%d.%s%st%d.%s",
                    tableIndex1 + tableOffset, attr1.getName(), op.toString(),
                    tableIndex2 + tableOffset, attr2.getName());
        }
    }

    public QueryMore condition(Object attr, SimpleQuery.Op op, Object value) {
        Condition c = new Condition();
        c.attr = (SingularAttribute<?, ?>) attr;
        c.op = op;
        c.value = value;
        c.tableIndex = tableIndex;
        conditions.add(c);
        return this;
    }

    public QueryMore selectThisTable() {
        AttrInfo info = new AttrInfo();
        info.tableIndex = tableIndex;
        selects.add(info);
        return this;
    }

    public QueryMore selectCountThisTable() {
        AttrInfo info = new AttrInfo();
        info.tableIndex = tableIndex;
        info.function = "count";
        selects.add(info);
        return this;
    }

    @SuppressWarnings("unchecked")
    public QueryMore select(Object... attrs) {
        for (int i = 0; i < attrs.length; i++) {
            AttrInfo info = new AttrInfo();
            info.attr = (SingularAttribute<?, ?>) attrs[i];
            info.tableIndex = tableIndex;
            selects.add(info);
        }
        return this;
    }

    public QueryMore selectSum(Object attr) {
        AttrInfo info = new AttrInfo();
        info.attr = (SingularAttribute<?, ?>) attr;
        info.function = "sum";
        info.tableIndex = tableIndex;
        selects.add(info);
        return this;
    }

    public QueryMore selectCount(Object attr) {
        AttrInfo info = new AttrInfo();
        info.attr = (SingularAttribute<?, ?>) attr;
        info.function = "count";
        info.tableIndex = tableIndex;
        selects.add(info);
        return this;
    }

    public QueryMore eq(Object attr, Object value) {
        return condition(attr, EQ, value);
    }

    // table0.uuid = table1.refUuid
    public TableOption<QueryMore> eq(Object attr) {
        ConditionBetweenTables condition = new ConditionBetweenTables();
        condition.tableIndex1 = tableIndex;
        condition.attr1 = (SingularAttribute<?, ?>) attr;
        condition.op = SimpleQuery.Op.EQ;

        TableOption<QueryMore> result = new TableOption<>();
        result.attrConsumer = (attr2) -> condition.attr2 = attr2;
        result.function = (tableIndex) -> {
            condition.tableIndex2 = tableIndex;
            moreConditions.add(condition);
            return this;
        };
        return result;
    }

    public QueryMore notEq(Object attr, Object value) {
        return condition(attr, NOT_EQ, value);
    }

    public QueryMore in(Object attr, Collection<?> collection) {
        DebugUtils.Assert(CollectionUtils.isNotEmpty(collection), "Op.IN value cannot be null or empty");
        return condition(attr, IN, collection);
    }

    public QueryMore in(Object attr, QueryMore subQuery) {
        return condition(attr, IN, subQuery);
    }

    public QueryMore in(Object attr, Q subQuery) {
        return in(attr, subQuery.toQueryMore());
    }

    public QueryMore notIn(Object attr, Collection<?> collection) {
        return condition(attr, NOT_IN, collection);
    }

    public QueryMore notIn(Object attr, QueryMore subQuery) {
        return condition(attr, NOT_IN, subQuery);
    }

    public QueryMore notIn(Object attr, Q subQuery) {
        return notIn(attr, subQuery.toQueryMore());
    }

    public QueryMore isNull(Object attr) {
        return condition(attr, NULL, null);
    }

    public QueryMore notNull(Object attr) {
        return condition(attr, NOT_NULL, null);
    }

    public QueryMore gt(Object attr, Object value) {
        return condition(attr, GT, value);
    }

    public QueryMore gte(Object attr, Object value) {
        return condition(attr, GTE, value);
    }

    public QueryMore lt(Object attr, Object value) {
        return condition(attr, LT, value);
    }

    public QueryMore lte(Object attr, Object value) {
        return condition(attr, LTE, value);
    }

    public QueryMore like(Object attr, Object value) {
        return condition(attr, LIKE, value);
    }

    public QueryMore notLike(Object attr, Object value) {
        return condition(attr, NOT_LIKE, value);
        }

    public QueryMore table(int tableIndex) {
        this.tableIndex = tableIndex;
        return this;
    }

    public QueryMore table0() {
        return table(0);
    }

    public QueryMore table1() {
        return table(1);
    }

    public QueryMore table2() {
        return table(2);
    }

    public QueryMore table3() {
        return table(3);
    }

    public QueryMore orderByAsc(Object attr) {
        OrderInfo order = new OrderInfo();
        order.tableIndex = tableIndex;
        order.od = SimpleQuery.Od.ASC;
        order.attr = (SingularAttribute<?, ?>) attr;
        orders.add(order);
        return this;
    }

    public QueryMore orderByDesc(Object attr) {
        OrderInfo order = new OrderInfo();
        order.tableIndex = tableIndex;
        order.od = SimpleQuery.Od.DESC;
        order.attr = (SingularAttribute<?, ?>) attr;
        orders.add(order);
        return this;
    }

    public QueryMore limit(int limit) {
        this.limit = limit;
        return this;
    }

    public QueryMore start(int start) {
        this.start = start;
        return this;
    }

    public String toSql() {
        return toSqlAndFillParamMap(new HashMap<>());
    }

    private String toSqlAndFillParamMap(Map<String, Object> params) {
        StringBuilder builder = new StringBuilder();

        builder.append("select ");
        if (!selects.isEmpty()) {
            for (AttrInfo select : selects) {
                builder.append(select.toSql()).append(',');
            }
            builder.deleteCharAt(builder.length() - 1);
        } else if (tables.length == 1) {
            builder.append("t0");
        } else {
            builder.append('*');
        }

        builder.append(" from ");
        for (int i = 0; i < tables.length; i++) {
            Class<?> table = tables[i];
            builder.append(table.getSimpleName()).append(' ').append('t').append(i + tableOffset).append(',');
        }
        builder.deleteCharAt(builder.length() - 1);

        if (!conditions.isEmpty() || !moreConditions.isEmpty()) {
            builder.append(" where ");
            List<String> conditionScripts = new ArrayList<>();

            for (Condition condition : conditions) {
                if (condition.hasSubQuery()) {
                    conditionScripts.add(condition.handleSubQuery(params, tableOffset + tables.length));
                    continue;
                }

                int paramIndex = params.size();
                conditionScripts.add(condition.toSql(paramIndex));
                if (condition.needParam()) {
                    params.put("p" + paramIndex, condition.value);
                }
            }

            for (ConditionBetweenTables moreCondition : moreConditions) {
                conditionScripts.add(moreCondition.toSql());
            }

            builder.append(String.join(" and ", conditionScripts));
        }

        if (!orders.isEmpty()) {
            builder.append(" order by ");

            for (OrderInfo order : orders) {
                builder.append(order.toSql()).append(',');
            }
            builder.deleteCharAt(builder.length() - 1);
        }

        if (limit != null) {
            builder.append(" limit ").append(limit);
        }
        if (start != null) {
            builder.append(" offset ").append(start);
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return toSql();
    }

    public SQL toSqlInstance(Class<?> returnType) {
        final Map<String, Object> paramMap = new HashMap<>();
        final SQL sqlInstance = SQL.New(toSqlAndFillParamMap(paramMap), returnType);
        paramMap.forEach(sqlInstance::param);
        return sqlInstance;
    }

    public SQL toSqlInstance() {
        final Map<String, Object> paramMap = new HashMap<>();
        final SQL sqlInstance = SQL.New(toSqlAndFillParamMap(paramMap));
        paramMap.forEach(sqlInstance::param);
        return sqlInstance;
    }

    public <T> List<T> list() {
        if (this.selects.size() == 1 && this.selects.get(0).attr == null) {
            int tableIndex = this.selects.get(0).tableIndex;
            return toSqlInstance(tables[tableIndex]).list();
        } else if (this.selects.isEmpty() && this.tables.length == 1) {
            return toSqlInstance(tables[0]).list();
        }

        return toSqlInstance().list();
    }

    public <T> T find() {
        if (this.selects.size() == 1 && this.selects.get(0).attr == null) {
            int tableIndex = this.selects.get(0).tableIndex;
            return toSqlInstance(tables[tableIndex]).find();
        } else if (this.selects.isEmpty() && this.tables.length == 1) {
            return toSqlInstance(tables[0]).find();
        }

        return toSqlInstance().find();
    }

    public <T> T find(Class<?> returnType) {
        return toSqlInstance(returnType).find();
    }

    public Tuple findTuple() {
        return toSqlInstance(Tuple.class).find();
    }

    public List<Tuple> listTuple() {
        return toSqlInstance(Tuple.class).list();
    }

    public Long count() {
        if (selects.isEmpty()) {
            AttrInfo info = new AttrInfo();
            info.tableIndex = 0;
            info.function = "count";
            selects.add(info);
        } else if (selects.size() == 1) {
            selects.get(0).function = "count";
        } else {
            throw new CloudRuntimeException("You must specify an attribute using the selectCount() method");
        }

        return toSqlInstance(Long.class).find();
    }

    public boolean isExists() {
        return count() > 0;
    }

    public static QueryMore New(Class<?> clz) {
        return new QueryMore(clz);
    }
}
