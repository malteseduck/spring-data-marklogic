package org.springframework.data.marklogic.domain;

import org.junit.Test;
import org.springframework.data.domain.PageImpl;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class ChunkRequestTests {

    @Test
    public void testPageNumber() {
        assertThat(new ChunkRequest(0, 20).getPageNumber()).as("first").isEqualTo(0);
        assertThat(new ChunkRequest(30, 20).getPageNumber()).as("second").isEqualTo(1);
        assertThat(new ChunkRequest(2, 2).getPageNumber()).as("third").isEqualTo(1);
    }

    @Test
    public void hasPrevious() throws Exception {
        assertThat(new ChunkRequest(0, 20).hasPrevious()).as("first").isFalse();
        assertThat(new ChunkRequest(30, 20).hasPrevious()).as("second").isTrue();
    }

    @Test
    public void isFirst() throws Exception {
        assertThat(new PageImpl<>(
                new ArrayList<String>(),
                new ChunkRequest(0, 20),
                40).isFirst()
        ).as("first").isTrue();
        assertThat(new PageImpl<>(
                new ArrayList<String>(),
                new ChunkRequest(30, 20),
                40).isFirst()
        ).as("second").isFalse();
    }
}
