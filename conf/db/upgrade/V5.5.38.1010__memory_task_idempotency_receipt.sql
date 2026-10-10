-- Preserve request identity after explicit terminal task-history deletion.
-- This migration neither deletes task history nor changes Host control state.
CREATE TABLE IF NOT EXISTS zstack.MemoryTaskIdempotencyReceiptVO (
    taskUuid VARCHAR(32) NOT NULL,
    requestKey VARCHAR(160) DEFAULT NULL,
    requestHash VARCHAR(64) DEFAULT NULL,
    originalStatus VARCHAR(32) NOT NULL,
    deletedDate DATETIME NOT NULL,
    PRIMARY KEY (taskUuid),
    UNIQUE KEY memory_task_receipt_request (requestKey)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
