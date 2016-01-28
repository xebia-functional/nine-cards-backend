CREATE TABLE Users (
  id serial NOT NULL PRIMARY KEY,
  email character varying(100),
  sessionToken character varying(100),
  banned BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE Devices (
  id serial NOT NULL PRIMARY KEY,
  userId BIGINT,
  deviceToken character varying(100),
  androidId character varying(100)
);