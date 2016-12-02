delete from sharedcollectionsubscriptions where userId in
  (select id from users where sessiontoken = 'bobtok') ;

delete from sharedcollections where userId in
  (select id from users where sessiontoken = 'anntok') ;

delete from installations where userId in
  (select id from users where sessiontoken in ('anntok', 'bobtok') ) ;

delete from users where sessiontoken in ('anntok','bobtok') ;
