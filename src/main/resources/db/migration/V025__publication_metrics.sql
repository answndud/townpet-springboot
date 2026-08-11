CREATE TABLE publication_metric (
    publication_id UUID NOT NULL REFERENCES publication(id),
    view_count BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT publication_metric_pk PRIMARY KEY (publication_id),
    CONSTRAINT publication_metric_view_ck CHECK (view_count >= 0)
);
