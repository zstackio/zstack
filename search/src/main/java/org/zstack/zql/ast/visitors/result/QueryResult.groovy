package org.zstack.zql.ast.visitors.result

class QueryResult {
    String sql
    ReturnWithResult returnWith
    List<FilterByResult> filterBy
}
