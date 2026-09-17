package com.eneik.epidemiology.document;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public interface DocumentSearchResultProjection {
    Long getId();
    String getTitle();
    String getDocType();
    String getAuthorOrganization();
    Integer getPublicationYear();
    LocalDate getPublicationDate();
    String getFilePath();
    String getTextContent();
    OffsetDateTime getCreatedAt();
    Double getRelevanceScore();
}
