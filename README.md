[![Build Status](https://travis-ci.com/47deg/nine-cards-backend-google-play.svg?token=wCyq7egUjRaCj4pGSszm&branch=master)](https://travis-ci.com/47deg/nine-cards-backend-google-play)

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
   "categories": [
      "PERSONALIZATION"
   ]
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
         "categories": [
            "PERSONALIZATION"
         ]
      },
      {
         "title": "Google",
         "packageName":"com.google.android.googlequicksearchbox",
         "categories": [
            "TOOLS"
         ]
      }
   ]
}
```

Response codes:

| Code | Reason |
|------|--------|
| `200 OK` | All responses, even if all packages are errors |
