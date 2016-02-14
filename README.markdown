[![TravisCI](https://travis-ci.com/47deg/nine-cards-backend.svg?token=qhZYP7DCaKxDnpZ6xzmz&branch=master)](https://travis-ci.com/47deg/nine-cards-backend/)
[![Codacy Badge](https://api.codacy.com/project/badge/grade/34b25607022243aeb44910745ac6f21b)](https://www.codacy.com)
[![Codacy
Badge Coverage](https://api.codacy.com/project/badge/coverage/34b25607022243aeb44910745ac6f21b)](https://www.codacy.com)

# 9Cards Backend V2

**Table of Contents**

- [Summary](#Summary)
- [Prerequisites](#Prerequisites)
- [Execute](#Execute)
- [Database connection](#DatabaseConnection)
- [Authentication](#authentication)
- [Endpoints](#endpoints)
    - [Login](#Login)
    - [Update Installation](#Updateinstallation)
- [Postgress](#Postgress)
- [License](#License)


##Summary

This backend app contains all our public work. It's splitted in 3 different modules:

* api
* processes
* services

##Prerequisites

To compile the project:

* 	Install sbt

          $ brew install sbt
     	  
* 	Install [postgress](##postgress)
* 	Clone this GiHub project in your computer
    	
           $ git clone https://github.com/47deg/nine-cards-backend.git
    
*     [Connection database](##Databaseconnection)
	
##Execute

From project root directory run:

    $ sbt    
    > project api
    > run



##Database connection
Possible ways to define the database connection info:

1. Modifying the application.conf file:

	- Set the existing config values in db.default block to connect to a local database
 	- Run sbt

2. Passing the values through sbt command

	- Run sbt -Ddb.default.driver= ---  -Ddb.default.url=--- -Ddb.default.user=--- -Ddb.default.password=---

3. Setting environment variables

	- export DB_DEFAULT_DRIVER= ---
	- export DB_DEFAULT_URL= ---
	- export DB_DEFAULT_USER= ---
	- export DB_DEFAULT_PASSWORD= ---
	- Run sbt

##Authentication
```
X-Appsly-Application-Id : appId

X-Appsly-REST-API-Key : restAPIKey

X-Appsly-Session-Token : sessionToken

X-Android-ID : androidId

X-Android-Market-Localization : en-US
```

##Endpoints

###Login
Request

```
GET        /api/ninecards/login
```

    {
        "email": "john@test.com",
        "androidId": "1A2B3C4D5E6F",
        "oauthToken": "F6E5D4C3B2A1"
    }

Response

    {
    	"sessionToken: "11111a-2222b-333c-44444d"
    }

###Update installation
Request

```
PUT        /api/ninecards/installation
```

    {
    	"deviceToken": "1111a-2222b-33c-4444d" 
     }


Response

     { 
    	"androidId": "1A2B3C4D5E6F", 
    	"deviceToken": "1111a-2222b-33c-4444d" 
     }

   

##Postgress

###Installation:

    sudo apt-get install postgresql postgresql-contrib
    
    sudo apt-get install postgresql-client
    
    sudo apt-get install pgadmin3

###Steps:

1. Into account postgres

    	sudo -i -u postgres

2. You can get a Postgres prompt immediately by typing:

    	psql
       
3. Create new user 

        CREATE USER ninecards_user PASSWORD 'ninecards_pass';

4. Create database: 

        createdb ninecards

5. Grant permissions to the new user:

        GRANT ALL ON DATABASE ninecards TO ninecards_user;


6. Connect database: 

        psql ninecards

    
7. Exit out of the PostgreSQL prompt by typing:
	
    	\q
    

##License
Copyright (C) 2012 47 Degrees, LLC Reactive. http://47deg.com hello@47deg.com

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.