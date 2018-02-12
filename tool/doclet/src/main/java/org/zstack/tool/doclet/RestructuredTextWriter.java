package org.zstack.tool.doclet;

import java.util.Map;

/**
 */
public interface RestructuredTextWriter {
    void writeInventory(String outputPath, Map<String, InventoryDoc> docs);

    void writeApiMessage(String outputPath, Map<String, APIMessageDoc> docs);

    void writeApiEvent(String outputPath, Map<String, APIEventDoc> docs);
}
