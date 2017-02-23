package org.springframework.data.marklogic.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.Serializable;

/**
 * A request for a chunk of data with an offset and limit instead of page numbers and page sizes.
 */
public class ChunkRequest implements Pageable, Serializable {

    private final int offset;
    private final int limit;
    private final Sort sort;

    public ChunkRequest(int offset, int limit) {
        this(offset, limit, null);
    }

    public ChunkRequest(int offset, int limit, Sort sort) {
        this.offset = offset > 0 ? offset: 0;
        this.limit = limit;
        this.sort = sort;
    }

    @Override
    public int getPageNumber() {
        return getOffset() / getPageSize();
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public Pageable next() {
        return new ChunkRequest(getOffset() + getPageSize(), getPageSize(), getSort());
    }

    public ChunkRequest previous() {
        return getOffset() == 0 ? this : new ChunkRequest(getOffset() - getPageSize(), getPageSize(), getSort());
    }

    @Override
    public Pageable first() {
        return new ChunkRequest(0, getPageSize(), getSort());
    }

    @Override
    public boolean hasPrevious() {
        return getOffset() > 1;
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? previous() : first();
    }

    @Override
    public int getOffset() {
        return offset;
    }

    public Sort getSort() {
        return sort;
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ChunkRequest)) {
            return false;
        }

        ChunkRequest that = (ChunkRequest) obj;

        boolean sortEqual = this.sort == null ? that.sort == null : this.sort.equals(that.sort);

        return super.equals(that) && sortEqual;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + (null == sort ? 0 : sort.hashCode());
    }

    @Override
    public String toString() {
        return String.format("Chunk [start: %d, limit %d, sort: %s]", getOffset(), getPageSize(),
                sort == null ? null : sort.toString());
    }
}