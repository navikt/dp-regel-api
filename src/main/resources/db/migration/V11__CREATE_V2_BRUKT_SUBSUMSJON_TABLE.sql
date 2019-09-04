CREATE TABLE v2_subsumsjon_brukt (
    id char(26),
    behandlings_id char(26),
    arena_ts TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY(id),
    FOREIGN KEY(behandlings_id) REFERENCES v1_behov_behandling_mapping (id),
    created TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc')
)