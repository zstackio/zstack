package org.zstack.core.db;

import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.persistence.LockModeType;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A lease stored on an existing entity. Every operation locks and refreshes the
 * row before inspecting ownership. Callbacks run in that same short transaction;
 * they must only perform local database work and tolerate deadlock retries.
 */
public abstract class DatabaseLease<T> {
    private final Class<T> entityClass;

    protected DatabaseLease(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected abstract String getToken(T entity);
    protected abstract Timestamp getUntil(T entity);
    protected abstract void setLease(T entity, String token, Timestamp until);

    public final class Handle {
        private final DatabaseLease<T> lease = DatabaseLease.this;
        private final Object resourceId;
        private final String token;

        private Handle(Object resourceId, String token) {
            this.resourceId = Objects.requireNonNull(resourceId, "resourceId");
            this.token = Objects.requireNonNull(token, "token");
            if (token.isEmpty()) {
                throw new IllegalArgumentException("lease token must not be empty");
            }
        }
    }

    public Handle handle(Object resourceId, String token) {
        return new Handle(resourceId, token);
    }

    private T lockRow(DatabaseFacade db, Handle handle) {
        if (handle.lease != this) {
            throw new IllegalArgumentException("lease handle belongs to another resource type");
        }
        String selectById = String.format("select vo from %s vo where vo.%s = :id",
                entityClass.getSimpleName(), EntityMetadata.getPrimaryKeyField(entityClass).getName());
        T entity = SQL.New(selectById, entityClass).param("id", handle.resourceId)
                .lock(LockModeType.PESSIMISTIC_WRITE).find();
        if (entity != null) {
            // A locking query can return an entity already cached in this transaction.
            db.getEntityManager().refresh(entity, LockModeType.PESSIMISTIC_WRITE);
        }
        return entity;
    }

    public boolean tryAcquire(Handle handle, long durationMs) {
        if (durationMs <= 0) {
            throw new IllegalArgumentException("lease duration must be positive");
        }
        return new SQLBatchWithReturn<Boolean>() {
            @Override
            protected Boolean scripts() {
                T entity = lockRow(databaseFacade, handle);
                if (entity == null) {
                    return false;
                }
                Timestamp now = databaseFacade.getCurrentSqlTime();
                Timestamp until = getUntil(entity);
                if (getToken(entity) != null && (until == null || !until.before(now))) {
                    return false;
                }
                setLease(entity, handle.token, new Timestamp(Math.addExact(now.getTime(), durationMs)));
                return true;
            }
        }.execute();
    }

    public void release(Handle handle) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                T entity = lockRow(databaseFacade, handle);
                if (entity != null && handle.token.equals(getToken(entity))) {
                    setLease(entity, null, null);
                }
            }
        }.execute();
    }

    /**
     * For existing transactional commit hooks. The caller must keep all guarded
     * writes in this transaction and invoke this before modifying the lease row.
     */
    public T lockIfHeld(Handle handle, long minRemainingMs) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("lease-protected writes require an active transaction");
        }
        DatabaseFacade db = org.zstack.core.Platform.getComponentLoader().getComponent(DatabaseFacade.class);
        T entity = lockRow(db, handle);
        if (entity == null || !handle.token.equals(getToken(entity)) || getUntil(entity) == null) {
            return null;
        }
        Timestamp now = db.getCurrentSqlTime();
        return getUntil(entity).getTime() - now.getTime() >= Math.max(0L, minRemainingMs) ? entity : null;
    }

    public boolean executeIfHeld(Handle handle, Consumer<T> action) {
        return execute(handle, action, true);
    }

    /**
     * For operation-result bookkeeping and token-only legacy operations. An
     * expired token may finish bookkeeping until another owner replaces it;
     * this does not authorize lease-protected business writes or renew the lease.
     */
    public boolean executeIfOwned(Handle handle, Consumer<T> action) {
        return execute(handle, action, false);
    }

    private boolean execute(Handle handle, Consumer<T> action, boolean requireUnexpired) {
        Objects.requireNonNull(action, "action");
        return new SQLBatchWithReturn<Boolean>() {
            @Override
            protected Boolean scripts() {
                T entity = requireUnexpired ? lockIfHeld(handle, 0L) : lockRow(databaseFacade, handle);
                if (entity == null || !handle.token.equals(getToken(entity))) {
                    return false;
                }
                action.accept(entity);
                return true;
            }
        }.execute();
    }

    /** A point-in-time observation; use executeIfHeld or lockIfHeld for writes. */
    public boolean isHeld(Handle handle, long minRemainingMs) {
        return new SQLBatchWithReturn<Boolean>() {
            @Override
            protected Boolean scripts() {
                return lockIfHeld(handle, minRemainingMs) != null;
            }
        }.execute();
    }
}
