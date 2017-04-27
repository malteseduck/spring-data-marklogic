package org.springframework.data.marklogic.repository.query;

public enum SelectedMode {
    INCLUDE("include"),
    EXCLUDE("exclude"),
    ALL("all"),
    HIERARCHICAL("include-with-ancestors");

    private final String name;

    SelectedMode(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
