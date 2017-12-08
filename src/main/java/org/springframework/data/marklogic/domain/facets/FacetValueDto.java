package org.springframework.data.marklogic.domain.facets;

import com.marklogic.client.query.FacetValue;

public class FacetValueDto {
    private final String name;
    private final long count;

    public FacetValueDto(String name, long count) {
        this.name = name;
        this.count = count;
    }

    public FacetValueDto(FacetValue facet) {
        this(facet.getLabel(), facet.getCount());
    }

    public String getName() {
        return name;
    }

    public long getCount() {
        return count;
    }
}