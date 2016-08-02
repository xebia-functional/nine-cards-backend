[![Build Status](https://travis-ci.com/47deg/nine-cards-backend-google-play.svg?token=wCyq7egUjRaCj4pGSszm&branch=master)](https://travis-ci.com/47deg/nine-cards-backend-google-play)

# 9 Cards V2 - Google Play Backend

This repository contains the source code of the Nine-Cards Google-Play backend (NCGP) application.

NineCards is an Android launcher, that can organize the applications and the data in your android device.
It organizes these into several collections, which the user can modify or create.
The application also shows some relevant information about each android's app, that it
gets from Google's Play Store.
The Nine-Cards Google Play backend is a server-side, REST service application, that supports
the features for querying for some metadata of  the application from Google's Play Store servers,
and storing this information


## Developers Setup

In this section, we describe the steps you have to take to setup your computer for developing and
running the NCGP application.


### Redis Server

The NCGP uses a [redis cache](http://redis.io/) to store the information about the apps.
This makes it quicker to fetch the information once it has been obtained from Google's servers,
which also avoids excessive load on these servers. Thus, to run the application you need to install
redis, as follows:

    sudo apt-get install redis-server redis-tools

Once you have installed redis, you should check if the `redis-server` is active, with the
following command: `sudo service redis-server status`. If it is not active, you can restart it
with the command `sudo service redis-server restart`.

### Protocol Buffers

The NCGP obtains some information from a [Google API](https://android.clients.google.com/fdfe/details),
that is also used by the play store applications. For its API's, Google uses the
[Protocol Buffers](https://developers.google.com/protocol-buffers/) to serialise its answers.
The schema of the responses is specified in this [source file](src/main/protobuf/GooglePlay.proto).
To build the application, it is necessary to compile that file into a set of Java classes that represent
all the entities.

    sudo apt-get install protobuf-compiler

Once the compiler is installed, you can compile manually that file to see the Java classes created.

    mkdir -p src/java
    protoc --java_out=src/java/ src/main/protobuf/GooglePlay.proto

The `.java` file should be located in `src/java/com/fortysevendeg/googleplay/proto/GooglePlay.java`.

### Building and running the application

The NCGP is just an `sbt` project. Simply running `sbt test:compile` should compile the source
and test files.

However, some of the tests (integration tests) need to use some configuration values that are specified
in this [configuration file](src/test/resources/test.conf). Thus, to run the tests you should use the command

    sbt -Dconfig.file="src/test/resources/test.conf" test

These values are an `androidId` and a `token`, which are used for the tests.

## 9Cards Google Play Service

This service provides information to the Nine Cards v2 client about apps from Google Play.

### API

The endpoints need the following headers to be set:

| Header Name | Description | Default if Optional | Example |
|-------------|-------------|---------------------|---------|
| `X-Android-ID` | The ID of the device making the request | _Required_ | `3E4D5FE71C813A3E` |
| `X-Google-Play-Token` | The secret token for the Google Play Account for the user of the device  | _Required_ | `DQAAABQBAACJ...EETQE` |
| `X-Android-Market-Localization` | The Google Play locale for the request | `en-US` | `en-US` |

#### Individual Package Details

**Deprecated**

This request, with the `application/json` content type will be removed prior to release. It is available now for backwards-compatability with the current Nine Cards v2 client.

_It is a stripped down version of the original Appsly server response._

| Endpoint | Method | Content-Type |
|----------|--------|--------------|
| `/googleplay/package/<package>` | `GET` | `application/json` |

`<package>` is the Google Play package ID, such as `com.fortysevendeg.ninecardslauncher`

Example Response:

```json
{
   "docV2":{
      "title":"9 Cards Home Launcher",
      "creator":"47 Degrees LLC",
      "docid":"com.fortysevendeg.ninecardslauncher",
      "details":{
         "appDetails":{
            "appCategory":[
               "PERSONALIZATION"
            ],
            "numDownloads":"100,000+",
            "permission":[
               "android.permission.MANAGE_ACCOUNTS",
               "com.google.android.gm.permission.READ_CONTENT_PROVIDER"
            ]
         }
      },
      "aggregateRating":{
         "ratingsCount":5272,
         "oneStarRatings":496,
         "twoStarRatings":267,
         "threeStarRatings":614,
         "fourStarRatings":1215,
         "fiveStarRatings":2680,
         "starRating":4.008346080780029
      },
      "image":[

      ],
      "offer":[

      ]
   }
}
```

Response codes:

| Code | Reason |
|------|--------|
| `200 OK` | Successfully found `<package>` |
| `500 Internal Server Error` | Package not found |

---

The updated versioned endpoint will look as follows. Please note this is a work in progress and is subject to change. Suggestions and comments are welcome.

**Last updated:** 1 March 2016

| Endpoint | Method | Content-Type |
|----------|--------|--------------|
| `/googleplay/package/<package>` | `GET` | `application/vnd+47deg.9c.package.v1+json` |

Example JSON response:

```
{
   "title": "9 Cards Home Launcher",
   "packageName":"com.fortysevendeg.ninecardslauncher",
   "category": "PERSONALIZATION" 
}
```


Response codes:

| Code | Reason |
|------|--------|
| `200 OK` | Successfully found `<package>` |
| `404 Not Found` | Package not found |

#### Bulk Package Details

**Deprecated**

Similar to the Individual Package Details endpoint, this is also deprecated in favor of versioned content-type negotiation.

| Endpoint | Method | Content-Type |
|----------|--------|--------------|
| `/googleplay/packages/detailed` | `POST` | `application/json` |

Example request body:

```json
{  
   "items":[  
      "com.fortysevendeg.ninecardslauncher",
      "com.google.android.googlequicksearchbox",
      "com.package.does.not.exist"
   ]
}
```

Example response:

```
{  
   "errors":[  
      "com.package.does.not.exist"
   ],
   "items":[  
      {  
         "docV2":{  
            "title":"Google",
            "creator":"Google Inc.",
            "docid":"com.google.android.googlequicksearchbox",

	    ... document structure as above
      },
      {  
         "docV2":{  
            "title":"9 Cards Home Launcher",
            "creator":"47 Degrees LLC",
            "docid":"com.fortysevendeg.ninecardslauncher",

	    ... document structure as above
      }
   ]
}
```


Response codes:

| Code | Reason |
|------|--------|
| `200 OK` | All responses, even if all packages are errors |

---

The updated versioned endpoint will look as follows. Please note this is a work in progress and is subject to change. Suggestions and comments are welcome.

**Last updated:** 1 March 2016

| Endpoint | Method | Content-Type |
|----------|--------|--------------|
| `/googleplay/packages/detailed` | `POST` | `application/vnd+47deg.9c.bulkpackage.v1+json` |

Example request body:

```json
{  
   "items":[  
      "com.fortysevendeg.ninecardslauncher",
      "com.google.android.googlequicksearchbox",
      "com.package.does.not.exist"
   ]
}
```

Example JSON response:

```json
{
   "errors":[
      "com.package.does.not.exist"
   ],
   "items":[
      {
         "title": "9 Cards Home Launcher",
         "packageName":"com.fortysevendeg.ninecardslauncher",
         "category": "PERSONALIZATION" 
      },
      {
         "title": "Google",
         "packageName":"com.google.android.googlequicksearchbox",
         "category": "TOOLS" 
      }
   ]
}
```

Response codes:

| Code | Reason |
|------|--------|
| `200 OK` | All responses, even if all packages are errors |


#### Single Card

| Endpoint | Method | Content-Type |
|----------|--------|--------------|
| `/googleplay/cards/{packageName}` | `GET` | `application/json` |



`<package>` is the Google Play package ID, such as `com.fortysevendeg.ninecardslauncher`

Example of a successful Response :

```json
{
   "packageName": "com.fortysevendeg.ninecardslauncher",
   "title" : "9 Cards Home Launcher",
   "free": true,
   "icon": "http://apps.icon.nine.cards",
   "stars" : 3.14159265359,
   "downloads" : "18357 - 25000",
   "categories": ["PERSONALIZATION"]
}
```

Example of a not-found response (the body is not empty)

```json
    { "message" : "com.package.does.not.exist" }
```

Response codes:

| Code | Reason |
|------|--------|
| `200 OK` | Successfully found `<package>` |
| `404 Not Found` | Package not found |

#### List of Cards

| Endpoint | Method | Content-Type |
|----------|--------|--------------|
| `/googleplay/cards/` | `POST` | `application/json` |


Example of request:
```json
{
   "items":[
      "com.fortysevendeg.ninecardslauncher",
      "com.package.does.not.exist"
   ]
}
```

Example of a successful Response :

```json
{
  "missing" : [
      "com.package.does.not.exist"
  ]
  "apps" : [
      {
         "packageName": "com.fortysevendeg.ninecardslauncher",
         "title" : "9 Cards Home Launcher",
         "free": true,
         "icon": "http://apps.icon.nine.cards",
         "stars" : 3.14159265359,
         "downloads" : "18357 - 25000",
         "categories": ["PERSONALIZATION"]
      }
  ]
}
```

Response codes:

| Code | Reason |
|------|--------|
| `200 OK` | Successfully found `<package>` |

---




