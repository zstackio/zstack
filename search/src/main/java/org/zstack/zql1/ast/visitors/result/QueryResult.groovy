package org.zstack.zql1.ast.visitors.result

import org.zstack.zql1.ast.ZQLMetadata

class QueryResult {
    String sql
    String targetFieldName
    Closure createJPAQuery
    Closure createCountQuery
    ZQLMetadata.InventoryMetadata inventoryMetadata
    List<ReturnWithResult> returnWith
    List<FilterByResult> filterBy
}
