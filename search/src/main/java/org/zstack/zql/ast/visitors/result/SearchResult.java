package org.zstack.zql.ast.visitors.result;

import java.util.List;

/**
 * Hibernate Search 5 removed — incompatible with Jakarta namespace.
 * FullTextQuery references replaced with Object until upgrade to 7.x.
 */
public class SearchResult {

    public static class Search {
        Object query;
        String restrictSql;

        public Object getQuery() {
            return query;
        }

        public void setQuery(Object query) {
            this.query = query;
        }

        public String getRestrictSql() {
            return restrictSql;
        }

        public void setRestrictSql(String restrictSql) {
            this.restrictSql = restrictSql;
        }
    }

    public List<Search> searchs;

    public List<Search> getSearchs() {
        return searchs;
    }

    public void setSearchs(List<Search> searchs) {
        this.searchs = searchs;
    }
}
