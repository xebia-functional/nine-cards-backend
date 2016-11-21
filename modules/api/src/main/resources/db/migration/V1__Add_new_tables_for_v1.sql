CREATE TABLE Users (
  id bigserial NOT NULL PRIMARY KEY,
  email character varying(100) NOT NULL,
  sessionToken character varying(100) NOT NULL,
  apiKey character varying(256) NOT NULL,
  banned BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE Installations (
  id bigserial NOT NULL PRIMARY KEY,
  userId BIGINT NOT NULL REFERENCES Users(id),
  deviceToken character varying(256),
  androidId character varying(100) NOT NULL,
  unique (userId, androidId)
);

CREATE TABLE SharedCollections (
  id bigserial NOT NULL PRIMARY KEY,
  publicIdentifier character varying(100) NOT NULL,
  userId BIGINT REFERENCES Users(id),
  publishedOn timestamp NOT NULL,
  author character varying(100) NOT NULL,
  name character varying(100) NOT NULL,
  views INTEGER NOT NULL DEFAULT 0,
  category character varying(64) NOT NULL,
  icon character varying(64) NOT NULL,
  community BOOLEAN NOT NULL DEFAULT FALSE,
  packages character varying(128)[] NOT NULL
);

CREATE TABLE SharedCollectionSubscriptions (
 sharedCollectionId BIGINT NOT NULL REFERENCES SharedCollections(id),
 userId BIGINT NOT NULL REFERENCES Users(id),
 sharedCollectionPublicId character varying(100) NOT NULL,
 PRIMARY KEY (sharedCollectionId, userId)
);
