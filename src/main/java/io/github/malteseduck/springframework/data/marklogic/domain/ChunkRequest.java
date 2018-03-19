package io.github.malteseduck.springframework.data.marklogic.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.Serializable;

/**
 * A request for a chunk of data options an offset and limit instead of page numbers and page sizes.
 */
public class ChunkRequest implements Pageable, Serializable {

    private final long offset;
    private final int limit;
    private final Sort sort;

    @Deprecated
    public ChunkRequest(long offset, int limit) {
        this(offset, limit, null);
    }

    @Deprecated
    public ChunkRequest(long offset, int limit, Sort sort) {
        this.offset = offset > 0 ? offset: 0;
        this.limit = limit;
        this.sort = sort;
    }

    @Override
    public int getPageNumber() {
        return getPageSize() == 0 ? 0 : Math.toIntExact(getOffset() / getPageSize());
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public Pageable next() {
        return ChunkRequest.of(getOffset() + getPageSize(), getPageSize(), getSort());
    }

    public ChunkRequest previous() {
        return getOffset() == 0 ? this : ChunkRequest.of(getOffset() - getPageSize(), getPageSize(), getSort());
    }

    @Override
    public Pageable first() {
        return ChunkRequest.of(0, getPageSize(), getSort());
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
    public long getOffset() {
        return offset;
    }

    public Sort getSort() {
        return sort;
    }

    public static ChunkRequest of(long offset, int limit) {
        return of(offset, limit, Sort.unsorted());
    }

    public static ChunkRequest of(long offset, int limit, Sort sort) {
        return new ChunkRequest(offset, limit, sort);
    }

    public static ChunkRequest of(long offset, int limit, Sort.Direction direction, String... properties) {
        return of(offset, limit, Sort.by(direction, properties));
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