package com.eneik.epidemiology.document;

public interface DocumentSearchResult {
    Document getDocument();
    Double getRelevanceScore();
}
