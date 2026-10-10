-- Persist Host task failure notification intent with the task's Failed transition.
CREATE TABLE zstack.MemoryTaskFailureOutboxVO (
    taskUuid VARCHAR(32) NOT NULL,
    hostUuid VARCHAR(32) NOT NULL,
    action VARCHAR(32) NOT NULL,
    reason VARCHAR(2048) DEFAULT NULL,
    createDate TIMESTAMP NOT NULL,
    leaseOwner VARCHAR(255) DEFAULT NULL,
    leaseUntil BIGINT DEFAULT NULL,
    attempts INT NOT NULL DEFAULT 0,
    delivered BIT NOT NULL DEFAULT 0,
    lastError VARCHAR(512) DEFAULT NULL,
    lastOpDate TIMESTAMP NOT NULL,
    PRIMARY KEY (taskUuid),
    KEY idxMemoryTaskFailureOutboxDelivery (delivered, leaseUntil, createDate)
);

-- One durable ZWatch event receipt per failed task and subscription. EventRecordsVO
-- and this receipt are committed atomically by the dedicated ZWatch consumer.
CREATE TABLE zstack.MemoryTaskFailureReceiptVO (
    taskUuid VARCHAR(32) NOT NULL,
    subscriptionUuid VARCHAR(32) NOT NULL,
    dataUuid VARCHAR(32) NOT NULL,
    suppressed BIT NOT NULL DEFAULT 0,
    createDate TIMESTAMP NOT NULL,
    PRIMARY KEY (taskUuid, subscriptionUuid),
    UNIQUE KEY ukMemoryTaskFailureReceiptDataUuid (dataUuid)
);
