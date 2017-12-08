package org.springframework.data.marklogic.domain.facets;

import com.marklogic.client.query.FacetResult;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FacetResultDto {
    private final String name;

    private final long count;

    private final List<FacetValueDto> values;

    public FacetResultDto(FacetResult result) {
        name = result.getName();
        values = Arrays.stream(result.getFacetValues())
                .map(FacetValueDto::new)
                .collect(Collectors.toList());
        count = values.size();
    }

    public String getName() {
        return name;
    }

    public long getCount() {
        return count;
    }

    public List<FacetValueDto> getValues() {
        return values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FacetResultDto that = (FacetResultDto) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "FacetResultDto{" +
                "name='" + name + '\'' +
                ", count=" + count +
                ", values=" + values +
                '}';
    }
}