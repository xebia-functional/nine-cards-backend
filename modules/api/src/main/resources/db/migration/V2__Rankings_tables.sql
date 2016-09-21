/* Note: a procedural-like solution was found and used
in a previous version, but it does not work with H2 */


/*World: called earth*/
create table if not exists rankings_earth (
  packageName character varying(256) not null,
  category varchar(30) not null,
  ranking integer not null,
  primary key (packageName, category)
);

/* Continents, without any word, just the name*/
create table if not exists rankings_africa (
  packageName character varying(256) not null,
  category varchar(30) not null,
  ranking integer not null,
  primary key (packageName, category)
);

create table if not exists rankings_america (
  packageName character varying(256) not null,
  category varchar(30) not null,
  ranking integer not null,
  primary key (packageName, category)
);

create table if not exists rankings_asia (
  packageName character varying(256) not null,
  category varchar(30) not null,
  ranking integer not null,
  primary key (packageName, category)
);

create table if not exists rankings_europe (
  packageName character varying(256) not null,
  category varchar(30) not null,
  ranking integer not null,
  primary key (packageName, category)
);

create table if not exists rankings_oceania (
  packageName character varying(256) not null,
  category varchar(30) not null,
  ranking integer not null,
  primary key (packageName, category)
);

/** Countries: with the word country before, and the two-letter code
*/
create table if not exists rankings_country_es (
  packageName character varying(256) not null,
  category varchar(30) not null,
  ranking integer not null,
  primary key (packageName, category)
);

create table if not exists rankings_country_gb (
  packageName character varying(256) not null,
  category varchar(30) not null,
  ranking integer not null,
  primary key (packageName, category)
);

create table if not exists rankings_country_us (
  packageName character varying(256) not null,
  category varchar(30) not null,
  ranking integer not null,
  primary key (packageName, category)
);

