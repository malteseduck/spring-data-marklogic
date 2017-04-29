package org.springframework.data.marklogic.core.mapping;

import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Enumeration;

/**
 * First attempt to return an InputStream of documents from the database to optimize returning of results if no business
 * logic is necessary.  Or some other method of deserialization can be used with the stream.
 *
 * Needed a "special" type so that the query builder could return that type.  The internals return a SequenceInputStream
 * since each document had to be a separate stream to get the correct format (instead of search response XML).
 * @param <T>
 */
public class DocumentStream<T> extends SequenceInputStream {

    public DocumentStream(Enumeration<? extends InputStream> e) {
        super(e);
    }

    public DocumentStream() {
        super(null);
    }
}
