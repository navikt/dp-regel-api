CREATE TABLE IF NOT EXISTS v2_subsumsjon
(
    behov_id VARCHAR(26)              NOT NULL,
    data     JSONB                    NOT NULL,
    PRIMARY KEY (behov_id),
    FOREIGN KEY (behov_id) REFERENCES v2_behov (id),
    created  TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc')
);


