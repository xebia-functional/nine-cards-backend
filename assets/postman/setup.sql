insert into users(email, sessiontoken, apikey, banned) values
  ('ann@gmail.com', 'anntok', 'annapikey', false) ;

insert into users(email, sessiontoken, apikey, banned) values
  ('bob@gmail.com', 'bobtok', 'bobapikey', false) ;

insert into installations(userid, devicetoken, androidid) values (
  (select id from users where sessiontoken='anntok'), 'anndevtok', 'anndroidid'
);

insert into installations(userid, devicetoken, androidid) values (
  (select id from users where sessiontoken='bobtok'), 'bobdevtok', 'bobdroidid'
);

insert into sharedcollections(publicidentifier, userid, publishedOn, author, name, views, category, icon, community, packages) values
  ('anncol',
    (select id from users where sessiontoken = 'anntok'),
    now(),
    'Ann',
    'Collection of Ann', 0, 'SOCIAL', '', false,
    '{ "com.android.chrome", "com.linkedin.android" }'
);

insert into sharedcollectionsubscriptions(sharedcollectionid, sharedCollectionPublicId, userid )
  select C.id, C.publicIdentifier, SU.id
  from
    (sharedcollections as C inner join users as AU on AU.id = C.userId) cross join users as SU 
  where AU.sessionToken = 'anntok' and SU.sessionToken = 'bobtok'
;
