CREATE TABLE IF NOT EXISTS V1_BEHOV
(
  id      VARCHAR(64)              NOT NULL,
  data    JSONB                    NOT NULL,
  PRIMARY KEY (id),
  created TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc')
);

CREATE TABLE IF NOT EXISTS V1_SUBSUMSJON
(
  id      VARCHAR(64)              NOT NULL,
  behovId VARCHAR(64)              NOT NULL,
  data    JSONB                    NOT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (behovId) REFERENCES V1_BEHOV(id),
  created TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc')
);


