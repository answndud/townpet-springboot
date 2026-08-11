CREATE TABLE web_vital_metric (
    id UUID NOT NULL,
    metric_name VARCHAR(20) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    route VARCHAR(200) NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT web_vital_metric_pk PRIMARY KEY (id),
    CONSTRAINT web_vital_metric_value_ck CHECK (metric_value >= 0)
);
CREATE INDEX web_vital_metric_observed_ix ON web_vital_metric (metric_name, observed_at DESC);
