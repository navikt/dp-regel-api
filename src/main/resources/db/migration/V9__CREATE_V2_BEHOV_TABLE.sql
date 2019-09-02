CREATE TABLE IF NOT EXISTS V2_BEHOV
(
    id                                CHAR(26)                 NOT NULL, -- ULID is always 26 chars
    behandlings_id                     CHAR(26)                 NOT NULL, -- ULID is always 26 chars
    aktor_id                          VARCHAR(20)              NOT NULL, -- TODO: length of aktor id ?
    beregnings_dato                   DATE                     NOT NULL,
    oppfyller_krav_til_fangst_og_fisk BOOLEAN                  NULL,
    avtjent_verne_plikt               BOOLEAN                  NULL,
    brukt_opptjening_forste_maned     DATE                     NULL,
    brukt_opptjening_siste_maned      DATE                     NULL,
    antall_barn                       NUMERIC                  NULL,
    manuelt_grunnlag                  NUMERIC                  NULL,
    data                              JSONB                    NOT NULL,
    created                           TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc'),
    PRIMARY KEY (id),
    FOREIGN KEY (behandlings_id) REFERENCES v1_behov_behandling_mapping (id)
);