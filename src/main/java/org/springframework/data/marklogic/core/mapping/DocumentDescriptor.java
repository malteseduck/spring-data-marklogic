package org.springframework.data.marklogic.core.mapping;

import com.marklogic.client.document.DocumentRecord;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.marker.ContentHandle;

public class DocumentDescriptor {

    private String uri;
    private DocumentMetadataHandle metadata;
    private Format format;

    // TODO - is there a "generic" way to do this for read/write?
    private ContentHandle content;
    private DocumentRecord record;

    public DocumentDescriptor() {}

    public DocumentDescriptor(String uri) {
        this.uri = uri;
    }

    public DocumentDescriptor(String uri, ContentHandle content) {
        this.uri = uri;
        this.content = content;
    }

    public DocumentDescriptor(DocumentRecord record) {
        this.uri = record.getUri();
        this.record = record;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public ContentHandle getContent() {
        return content;
    }

    public void setContent(ContentHandle content) {
        this.content = content;
    }

    public DocumentRecord getRecord() {
        return record;
    }

    public void setRecord(DocumentRecord record) {
        this.record = record;
    }

    public DocumentMetadataHandle getMetadata() {
        return metadata;
    }

    public void setMetadata(DocumentMetadataHandle metadata) {
        this.metadata = metadata;
    }

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }
}
