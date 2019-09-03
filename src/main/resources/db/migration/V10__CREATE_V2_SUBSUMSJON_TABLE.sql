CREATE TABLE IF NOT EXISTS v2_subsumsjon
(
    behov_id CHAR(26)                 NOT NULL, -- ULID is always 26 chars
    data     JSONB                    NOT NULL,
    PRIMARY KEY (behov_id),
    FOREIGN KEY (behov_id) REFERENCES v2_behov (id),
    created  TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc')
);


