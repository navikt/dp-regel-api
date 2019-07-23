CREATE TABLE IF NOT EXISTS BEHOV
(
  id      VARCHAR(64)              NOT NULL,
  regel   VARCHAR(20),
  data    JSONB                    NOT NULL,
  PRIMARY KEY (id),
  created TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc')
);

CREATE TABLE IF NOT EXISTS SUBSUMSJON
(
  id      VARCHAR(64)              NOT NULL,
  regel   VARCHAR(20)              NOT NULL,
  behovId VARCHAR(64)              NOT NULL,
  data    JSONB                    NOT NULL,
  PRIMARY KEY (id, regel),
  FOREIGN KEY (behovId) REFERENCES BEHOV (id),
  created TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc')
);


