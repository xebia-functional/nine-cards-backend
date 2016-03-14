CREATE TABLE Users (
  id serial NOT NULL PRIMARY KEY,
  email character varying(100) NOT NULL,
  sessionToken character varying(100) NOT NULL,
  banned BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE Installations (
  id serial NOT NULL PRIMARY KEY,
  userId BIGINT NOT NULL REFERENCES Users(id) ,
  deviceToken character varying(100),
  androidId character varying(100) NOT NULL,
  unique (userId, androidId)
);

CREATE TABLE ShareCollection (
  id serial NOT NULL PRIMARY KEY,
  sharedCollectionId BIGINT NOT NULL,
  publishedOn date,
  description character varying(100),
  author character varying(100) NOT NULL,
  name character varying(100) NOT NULL,
  shareLink character varying(100) NOT NULL,
  packages text ARRAY,
  lat double precision NOT NULL DEFAULT 0,
  lng double precision NOT NULL DEFAULT 0,
  alt double precision NOT NULL DEFAULT 0,
  views INTEGER NOT NULL,
  category character varying(100),
  icon character varying(100),
  community BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE SharedCollectionSubscription(
 id serial NOT NULL PRIMARY KEY,
 sharedCollectionId BIGINT NOT NULL,
 userId BIGINT NOT NULL
);
