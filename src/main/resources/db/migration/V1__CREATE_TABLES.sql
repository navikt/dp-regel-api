CREATE TABLE IF NOT EXISTS BEHOV
(
  id       VARCHAR(64) PRIMARY KEY,
  status   VARCHAR(7)               NOT NULL,
  data     JSONB                    NOT NULL,
  created  TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc'),
  modified TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc')
);

CREATE OR REPLACE FUNCTION update_modified_column()
  RETURNS TRIGGER AS
$$
BEGIN
  NEW.modified = now() at time zone 'utc';
  RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_behov_modtime
  BEFORE UPDATE
  ON BEHOV
  FOR EACH ROW
EXECUTE PROCEDURE update_modified_column();


CREATE TABLE IF NOT EXISTS SUBSUMSJON
(
  id      VARCHAR(64)              NOT NULL,
  regel   VARCHAR(20)              NOT NULL,
  behovId VARCHAR(64) REFERENCES BEHOV (id),
  data    JSONB                    NOT NULL,
  PRIMARY KEY (id, regel),
  created TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc')
);

create TABLE IF NOT EXISTS ARENA_VEDTAK_BEHOV_MAPPING
(
  id       SERIAL PRIMARY KEY,
  vedtakId INTEGER                  NOT NULL,
  behovId  VARCHAR(64) REFERENCES BEHOV (id),
  created  TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc')
)

