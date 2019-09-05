CREATE INDEX minsteinntektresultat_idx ON v2_subsumsjon((data->'minsteInntektResultat' ->> 'subsumsjonsId'));
CREATE INDEX perioderesultat_idx ON v2_subsumsjon((data->'periodeResultat' ->> 'subsumsjonsId'));
CREATE INDEX satsresultat_idx ON v2_subsumsjon((data->'satsResultat' ->> 'subsumsjonsId'));
CREATE INDEX grunnlagresultat_idx ON v2_subsumsjon((data->'grunnlagResultat' ->> 'subsumsjonsId'));