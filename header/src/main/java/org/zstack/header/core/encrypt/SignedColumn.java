package org.zstack.header.core.encrypt;

import java.util.List;
import java.util.Map;

/**
 * @Author: DaoDao
 * @Date: 2021/10/29
 */
public class SignedColumn {
    private List<String> signedColumnNames;
    private Map<String, String> appointMap;
    private String primaryKey;
    private String tableName;

    public List<String> getSignedColumnNames() {
        return signedColumnNames;
    }

    public void setSignedColumnNames(List<String> signedColumnNames) {
        this.signedColumnNames = signedColumnNames;
    }

    public Map<String, String> getAppointMap() {
        return appointMap;
    }

    public void setAppointMap(Map<String, String> appointMap) {
        this.appointMap = appointMap;
    }

    public String getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
}
