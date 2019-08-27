CREATE TABLE IF NOT EXISTS v1_behov_ekstern_mapping
(
    id         CHAR(26)                 NOT NULL, -- ULID is always 26 char length
    ekstern_id VARCHAR(64)              NOT NULL,
    kontekst   VARCHAR(16)              NOT NULL,
    PRIMARY KEY (id),
    created    TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc')
);

CREATE UNIQUE INDEX IF NOT EXISTS uniq_ekstern_kontekst ON v1_behov_ekstern_mapping (ekstern_id, kontekst);