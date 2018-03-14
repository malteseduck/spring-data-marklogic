package org.malteseduck.springframework.data.marklogic.domain;

import org.junit.Test;
import org.springframework.data.domain.PageImpl;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class ChunkRequestTests {

    @Test
    public void testPageNumber() {
        assertThat(ChunkRequest.of(0, 20).getPageNumber()).as("first").isEqualTo(0);
        assertThat(ChunkRequest.of(30, 20).getPageNumber()).as("second").isEqualTo(1);
        assertThat(ChunkRequest.of(2, 2).getPageNumber()).as("third").isEqualTo(1);
    }

    @Test
    public void hasPrevious() throws Exception {
        assertThat(ChunkRequest.of(0, 20).hasPrevious()).as("first").isFalse();
        assertThat(ChunkRequest.of(30, 20).hasPrevious()).as("second").isTrue();
    }

    @Test
    public void isFirst() throws Exception {
        assertThat(new PageImpl<>(
                new ArrayList<String>(),
                ChunkRequest.of(0, 20),
                40).isFirst()
        ).as("first").isTrue();
        assertThat(new PageImpl<>(
                new ArrayList<String>(),
                ChunkRequest.of(30, 20),
                40).isFirst()
        ).as("second").isFalse();
    }
}
