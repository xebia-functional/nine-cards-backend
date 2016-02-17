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