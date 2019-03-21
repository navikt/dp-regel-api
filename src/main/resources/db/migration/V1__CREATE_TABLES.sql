CREATE TABLE IF NOT EXISTS SUBSUMSJON (
  ulid       VARCHAR(64) PRIMARY KEY,
  data       JSONB NOT NULL,
  timestamp  TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc')
);