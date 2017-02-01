/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
