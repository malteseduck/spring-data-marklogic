package io.github.malteseduck.springframework.data.marklogic.domain.facets;

import com.marklogic.client.query.FacetResult;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Extension of Spring's {@link org.springframework.data.domain.Page} that adds MarkLogic's facet results to a search
 * result page.
 *
 * @param <T>
 */
public class FacetedPage<T> extends PageImpl<T> {

    private List<FacetResultDto> facets;

    public FacetedPage(List<T> content, Pageable pageable, long total, FacetResult[] facets) {
        super(content, pageable, total);
        this.facets = Arrays.stream(facets)
                .map(FacetResultDto::new)
                .collect(Collectors.toList());
    }

    public List<FacetResultDto> getFacets() {
        return facets;
    }
}