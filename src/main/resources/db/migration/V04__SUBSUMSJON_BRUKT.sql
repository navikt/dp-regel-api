CREATE TABLE IF NOT EXISTS v1_subsumsjon_brukt
(
    id VARCHAR(64) NOT NULL,
    ekstern_id VARCHAR(64) NOT NULL,
    kontekst VARCHAR(20) NOT NULL,
    arena_ts TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY(id),
    FOREIGN KEY(id) REFERENCES SUBSUMSJON (id),
    created TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc')
);