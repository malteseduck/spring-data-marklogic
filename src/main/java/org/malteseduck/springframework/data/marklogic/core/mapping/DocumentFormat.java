package org.malteseduck.springframework.data.marklogic.core.mapping;

import com.marklogic.client.io.Format;

// TODO: Is it good to abstract away the MarkLogic-specific format for "supported" formats, or should we just use that since this library is MarkLogic-specific anyway?
public enum DocumentFormat {
    JSON,
    XML;

    public static DocumentFormat parse(Format format) {
        switch (format) {
            case XML: return XML;
            case UNKNOWN:
            case BINARY:
            case TEXT:
                throw new IllegalArgumentException("No supported document format for '" + format + "'");
            default: return JSON;
        }
    }

    public Format toFormat() {
        switch (this) {
            case XML: return Format.XML;
            default: return Format.JSON;
        }
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
