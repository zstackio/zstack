package org.zstack.zql.ast.visitors.result

import org.zstack.zql.ast.ZQLMetadata

class QueryResult {
    String sql
    String targetFieldName
    Closure createJPAQuery
    Closure createCountQuery
    ZQLMetadata.InventoryMetadata inventoryMetadata
    List<ReturnWithResult> returnWith
    List<FilterByResult> filterBy
}
