package org.zstack.xinfini.sdk;

public class XInfiniQueryResponse extends XInfiniResponse {
    private QueryResponseMetadata metadata;

    public QueryResponseMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(QueryResponseMetadata metadata) {
        this.metadata = metadata;
    }

    public static class QueryResponseMetadata {
        private Pagination pagination;

        public Pagination getPagination() {
            return pagination;
        }

        public void setPagination(Pagination pagination) {
            this.pagination = pagination;
        }

        public static class Pagination {
            protected long totalCount;
            protected long count;
            protected int offset;
            protected int limit;

            public long getTotalCount() {
                return totalCount;
            }

            public void setTotalCount(long totalCount) {
                this.totalCount = totalCount;
            }

            public long getCount() {
                return count;
            }

            public void setCount(long count) {
                this.count = count;
            }

            public int getOffset() {
                return offset;
            }

            public void setOffset(int offset) {
                this.offset = offset;
            }

            public int getLimit() {
                return limit;
            }

            public void setLimit(int limit) {
                this.limit = limit;
            }
        }
    }
}
