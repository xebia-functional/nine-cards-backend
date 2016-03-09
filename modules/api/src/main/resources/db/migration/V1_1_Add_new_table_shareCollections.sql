CREATE TABLE ShareCollection (
  id serial NOT NULL PRIMARY KEY,
  sharedCollectionId BIGINT NOT NULL,
  publishedOn date,
  description character varying(100)
);