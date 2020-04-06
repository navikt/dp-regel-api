DROP INDEX IF EXISTS minsteinntektresultat_idx;
CREATE INDEX minsteinntektresultat_idx_2 ON v2_subsumsjon((data->'minsteinntektResultat' ->> 'subsumsjonsId'));