[![TravisCI](https://travis-ci.com/47deg/nine-cards-backend.svg?token=qhZYP7DCaKxDnpZ6xzmz&branch=master)](https://travis-ci.com/47deg/nine-cards-backend/)
[![Codacy Badge](https://api.codacy.com/project/badge/grade/34b25607022243aeb44910745ac6f21b)](https://www.codacy.com)
[![Codacy Badge Coverage](https://api.codacy.com/project/badge/coverage/34b25607022243aeb44910745ac6f21b)](https://www.codacy.com)

# 9Cards Backend V2

This repository contains the source code of the Nine-Cards Back-End (NCBE) application.

NineCards is an Android Launcher that organises the applications and data of your phone into several collections.
The user can modify these collections, create new collections. Also, a user can publish and curate a shared collection
which other users can subscribe to.

The NineCards Back End (NCBE) is a server-side, REST service application, that supports the features for publishing and
subscribing to Shared Collections.

<!-- markdown-toc start - Don't edit this section. Run M-x markdown-toc-generate-toc again -->
**Table of Contents**
- [9Cards Backend V2](#9cards-backend-v2)
    - [Description of the Application](#description-of-the-application)
        - [Data Model](#data-model)
        - [Authentication and Authorization](#authentication-and-authorization)
            - [Client Signup in BackEnd](#client-signup-in-backend)
            - [User Authentication](#user-authentication)
            - [Manually obtaining an IdToken](#manually-obtaining-an-idtoken)
    - [Developers Setup](#developers-setup)
        - [Scala Development Tools](#scala-development-tools)
        - [Postgres Database Setup](#postgres-database-setup)
            - [Installation](#installation)
            - [Setting Client authentication](#setting-client-authentication)
            - [Setting user and password for local development:](#setting-user-and-password-for-local-development)
            - [Database Schema Migrations](#database-schema-migrations)
    - [Running and testing the application](#running-and-testing-the-application)
        - [Database Connection Configuration](#database-connection-configuration)
        - [Testing and running the endpoints with Postman](#testing-and-running-the-endpoints-with-postman)
    - [Deployment - Preparing the Application](#deployment---preparing-the-application)
    - [License](#license)
<!-- markdown-toc end -->


## Description of the Application

In NineCards Version 2, most of the user's data and configuration (such as her collections) is kept in her Google Drive account,
and most of the functionality is provided either by the [client](https://github.com/47deg/nine-cards-v2/)
or by a separate [Google Play integration](https://github.com/47deg/nine-cards-backend-google-play) subsystem.
The Back End is only concerned with those features that involve information shared between several users.

The NCBE is organised within three modules, called `api`, `processes`, and `services`:

* The **api** module implements the HTTP server application that provides the endpoints for the application.
  The routes are built with the [spray](http://spray.io/) library, with [spray-json](http://github.com/spray/spray-json) serialization.

* The **processes** module implements the _business logic_ behind each endpoint. This business logic is implemented in a style from functional programming,
  based on the use of monadic operations.

* The **services** module provides the basic operations for reading and writing the objects of the data model in the database.
  The operations are built with [doobie](https://github.com/tpolecat/doobie), which is a functional-style wrapper around JDBC.

The processes and services follow a design inspired by the Scala libraries [scalaz](https://github.com/scalaz/scalaz)
and [cats](http://typelevel.org/cats/).

### Data Model

The data model, that is the entities and relations handled by the application, is represented in the database
using the scheme definitions in [the migrations files](modules/api/src/main/resources/db/migration/).
It consists of five main classes: `User`, `Installation`, `Collection`, `Package`, and `Subscription`.

A **User** represents an entity, usually a person, who owns a Google account.
* The `sessionToken` is a unique identifier of the user within the NCBE, as an alternative to the user's email.
  It is used in the communication for the communication between the NCBE and the client, to _hide_ the actual user.
* The `apiKey` is a private cryptographic key for the client, generated at signup by the NCBE.

An **Installation** represents a device (e.g. a mobile phone) of a *User*, in which NCBE has been installed.
* There is a `1:N` relation between **User** and **Installation**. Thus, every installation belongs to one user,
  and a user can own one or more installations.
* The `androidId` is a globally unique identifier of each Android device.
* The `deviceId` is used for _push_ notifications services.

A **Collection** is a curated set of Android applications. The collection is annotated with some information
about who wrote and when it, what kind of applications it containsait contains, some statistics about visits
and downloads, etc.
NCBE only deals with **shared** collections, those that one user publishes and curates and other users
subscribe to. In this data model, every collection is a *shared* collections published by a user.
Thus, there is a `1:N` relation between **user** and **collection**. Every collection has one author,
which is marked in the field `userId`. However, a user can be the author of several or none collections.

A **Package** represents an application that is included in a collection.
The field `packageName` represents the application as the Java package of its main executable
class, e.g. `com.fortysevendeg.ninecardslauncher.app.App`.

A **Subscription** is an unconstrained `M:N` relation between user and collections, where each entry
indicates that a user is subscribed to a collection.


### Authentication and Authorization


#### Client Signup in BackEnd

We describe now the process of interactions performed for a client (user and device) to sign up in the NCBE.
This process is not performed unless a user wants to publish a new shared collection, or subscribe to an existing shared collection.
The process involves a series of interactions between three subsystems: the NCBE,
the **Google Account Services** (GAC), and the **Client**, which is the Ninecards Android application in
the user's device).

1. **Grant Google Account Permissions.**
   The *Client* sends a request to the GAC to open an authorization token. The request carries the user's `email` and the `deviceId`,
   which uniquely identify the instance of the NineCardsClient issuing the request.
   If the request is accepted, the GAC responds with success and includes in the response an `idToken`, which identifies a short-lived OAuth session within the GAC.

2. **Client signup in NCBE.**
   The *Client* sends a HTTP request to the NCBE's endpoint `POST {apiRoot}/login`, to register in it.
   This request carries (as a JSon object in the body entity) three fields:
   * the `email` that serves as an external identifier for the user running the client,
   * the `androidId`, that identifies the Android device in which the user is running the client application, and
   * the `idToken` that the client received from the GAC in Step 1.
   If the request succeeds, the NCBE records the client's user and device and returns a response to the client app.
   The response carries (in a Json object in the request body) two fields: a `sessionToken`, and an `apiKey`.
   * The `sessionToken` is a unique identifier of the user within the NCBE instead of the user's email. 
   * The `apiKey` is the private cryptographic key that the client uses after signup to authenticate in the NCBE.

3. **NCBE access to GAC.**
   To carry out the process of endpoint `POST {apiRoot}/login`, the NCBE communicates to the GAC
   to validate the `idToken` included in the client's request.
   If the `idToken` is validated, the GAC returns a successful response.

#### User Authentication

All NCBE endpoints, except from the one to read the API documentation and the one to signup,
carry out an authentication step, to check that the client app sending the requests is acting for a registered user.
The information for user authentication is carried in the HTTP headers `X-Android-ID`, `X-Auth-Token` and `X-Session-Token`.

* The `X-Android-ID` should give the `androidId` of the client's device. Note that, since the GAC process in the signup
  involve a user and device, the device itself and nut just the user should be signed up beforehand.

* The `X-Session-Token` should carry the user's `sessionToken` that the NCBE generated and gave to the client in Step 3 of the signup process.
  This value acts as a way to identify the user within the NCBE.

* The `X-Auth-Token` is a [Hash-based message authentication code](https://en.wikipedia.org/wiki/Hash-based_message_authentication_code), which is used for authenticating the request.
  It ensures that the client which is using the `sessionToken` is the one that is acting for that user.

The value of the `X-Auth-Token` header is computed as follows.
The *message* to be signed is just the full [URI](https://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.2.2) of the request, including protocol, host, port, path, and query.
The *cryptographic key* used is the user's `apiKey`, which the NCBE generates and gives to the client (with the `sessionToken`) at the end of the signup process.
The code is then calculated using the `sha512` hash function.
To calculate the value, you can use one of these methods:

* The command `openssl` can generate the HMAC digest of a message. For example, to digest the URI `http://127.0.0.1:8080/collections/a` with the key `foo`, you would run:

        echo -n "http://127.0.0.1:8080/collections/a" | openssl dgst -sha512 -hmac "foo" | awk '{print $2}'

* This [web page](http://www.freeformatter.com/hmac-generator.html) offers a graphic dialog to generate the digest.
  The request URL would be into the `message` dialog, the user's `apiKey` would go into the `SecretKey` text box,
  and the algorithm would be "SHA512". The result would be the digest.

#### Manually obtaining an IdToken

Sometimes, you may want to carry out manually the first step of the signup, which is the communication between the
Client and the Google Account Services.

Google provides an [`OAuth 2.0 Playground`](https://developers.google.com/oauthplayground/), which can be used to generate a Google ID Token.
We use it to generate an OAuth token that, within the Google API, allows us to read the information needed for the client.
This information is only the user's email, so the scope used is `https://www.googleapis.com/auth/userinfo.email`.

1. Open the [OAuth 2.0 Playground page](https://developers.google.com/oauthplayground/).
   In this page, look in the lateral pane for the menu `Google+ API v1`, from this menu mark the scope `https://www.googleapis.com/auth/userinfo.email`,
   and push the `Authorize APIs` button.
2. If you have several Google accounts stored in the browser, you may be asked to select one. You will then
   be presented with a usual Google permissions dialog, asking you to allow an application to _Read your email address_.
    Press _Allow_.
3. You should open a new page showing the `Request/Response`. In the left pane there is a text field labelled _Authorization Code_,
   and below it there is a button labelled _Exchange authorization code for tokens_. Press this button.
4. The box labelled _Access Token_, which identifies a session within the _Google Account Services_ that will expire in an hour. This is the value you need for the `idToken`.


## Developers Setup

In this section, we describe the steps you have to take to setup your computer for developing and
running the Nine-Cards-Back-End (NCBE) application.

### Scala Development Tools

The NCBE is written in [Scala](www.scala-lang.org), using the [Scala Build Tool (SBT)](http://www.scala-sbt.org/) for automatic build.

*   Java SE 8 Development Kit: you can use [OpenJDK](http://openjdk.java.net/projects/jdk8/).
    In Ubuntu/Debian, if you have several versions of the JDK installed, you may need to use the `update-java-alternatives` program.
*   [Scala 2.11.8](http://www.scala-lang.org/download/2.11.8.html).
*   [SBT version 0.13.8](http://www.scala-sbt.org/download.html) or later.

If you have an older version of SBT, or Scala, often `sbt` can bootstrap itself to a newer versions.

You should put `scala`, `scalac`, and `sbt` are in PATH.

### Postgres Database Setup

The NCBE stores its information in a database, using the [[postgresql]] database system.
It uses a database called `ninecards`. To write, run, and test NCBE in your machine, you can create
a user `ninecards_user`, with password `ninecards_pass`.

#### Installation

In a Debian-based Linux distribution, you can use the `apt-get` command to install the packages

    sudo apt-get install postgresql postgresql-contrib postgresql-client pgadmin3

For OsX users, you can use any of the tools mentioned [here](http://www.postgresql.org/download/macosx/).

#### Setting Client authentication

In PostgreSQL, the "Client Authentication" method, used for opening a client's session, can be set differently for each user, database, or connection.
This configuration is kepy in a file called [`pg_hba.conf`](http://www.postgresql.org/docs/9.1/static/auth-pg-hba-conf.html).

* In Debian-based distributions, it is located in the directory `/etc/postgresql/{version}/main/pg_hba.conf`.
* In OSX, you can find it using the command `locate pg_hba.conf`, or following [these instructions](http://stackoverflow.com/questions/14025972/postgresql-how-to-find-pg-hba-conf-file-using-mac-os-x).

To run and test the NCBE on our local host using the `ninecards_user` user, we need to open channels for the command line and for the JDBC driver.

* The JDBC used by the NCBE enters the database trough a local IPv4 connection. To allow it, the following line should be in `pg_hba.conf`:

        host    ninecards       all  127.0.0.1/32            md5

* For setting up the database for tests, we want to enter the database from a shell terminal, using  the command `psql`
as the `ninecards_user`. To allow this, you should have the following line in `pg_hba.conf`:

        local   ninecards       ninecards_user                          md5

* You need to restart the Postgres server for the changes to take effect. To do this, run the following command in a terminal:

    	sudo service postgresql restart

#### Setting user and password for local development:

To create the `ninecards` database and the `ninecards_user` we need to open a session as the PostgreSQL-server administrator.
The administrator is the DBMS user called `postgres`, and by default it is configured to use  `peer` authentication.
Under this method, you can only open a DBMS session from a OS user with the same name.
Thus, you need to follow these steps:

1. Start `psql`, the PostgreSQL command-line client, as the `postgres` OS user:

    	sudo -u postgres psql

2. Inside `psql`, create the database, the user, the permissions, and exit.

        create database ninecards ;
        create user ninecards_user PASSWORD 'ninecards_pass';
        GRANT ALL ON DATABASE ninecards TO ninecards_user;
    	\q

3. From your own OS user, you should now be able to open a postgres-client session using the following command:

        psql --username=ninecards_user ninecards --password

#### Database Schema Migrations

The evolutions for the data schema of the `ninecards` database are managed by the build system of NCBE,
using the [flyway SBT plugin](https://flywaydb.org/documentation/sbt/).
Flyway needs the parameters to access the database. For setting your machine, you can use the values in `localhost.conf`.
For this, you should open a shell session in the `nine-cards-backend` root directory,
and start `sbt` using the `localhost.conf` file:

        sbt -Dconfig.file="modules/api/src/main/resources/localhost.conf"

You can use either the projects `api` or `root`.
Once the `sbt` session is opened, you can clear the database with the command
`flywayClean`, or you can perform the database schema migrations by running `flywayMigrate`.

**Note**: `flyway` connects to the database through JDBC, so you should configure the `pg_hba.conf` file as explained earlier.


## Running and testing the application

From a command line, within the root directory of the project, run the following:

    $ sbt -Dconfig.file="modules/api/src/main/resources/localhost.conf"
    > project api
    > run

To check that the application has started correctly, you can try accessing the Swagger apidocs in the
`http://localhost:8080/apiDocs` URL.

### Database Connection Configuration

The configuration is managed using Lightbend's [configuration library](https://github.com/typesafehub/config).
The default configuration is at the `modules/api/src/main/resources/application.conf` file, which
loads the values for some configuration settings from the environment. This gives you several ways to
define your configuration settings:

a. Run `sbt` passing the configuration settings, each setting having the form `-D{key}}={{value}}`, such as:

        sbt -Ddb.default.driver="..."  -Ddb.default.url="..." -Ddb.default.user="..." -Ddb.default.password="..."

b. Write a configuration file with your settings, and pass that file to `sbt` using the `-Dconfig.file` option, as follows:

        sbt -Dconfig.file="modules/api/src/main/resources/localhost.conf"

c. Set the shell environment variables used by the default configuration file. For instance, in `bash`, this is done as follows:

        export DB_DEFAULT_DRIVER="..."
        export DB_DEFAULT_URL="..."
        export DB_DEFAULT_USER="..."
        export DB_DEFAULT_PASSWORD="..."

    Of course, you can write such settings in the `.bashrc` file, or in a executable shell script.

### Testing and running the endpoints with Postman

Once the application is running and bound to the chosen port, you can run the endpoints by issuing HTTP
requests with any HTTP client, like [`curl`](https://en.wikipedia.org/wiki/CURL).
In particular, we use the [Postman](https://www.getpostman.com/) graphic client.
Postman allows us to write a collection of HTTP requests and store it as a text file.
These requests can depend on variables read from an environment that is also stored as a text file.

To test the endpoints of the application, we provide a [collection](assets/postman/collection.json) of Postman requests,
as well as an [environment](assets/postman/environment.json) for those requests.



## Deployment - Preparing the Application

The NCBE is a server side application, and it should be deployed as a
[Infrastracture as a Service (IaaS)](https://en.wikipedia.org/wiki/Cloud_computing#Infrastructure_as_a_service_.28IaaS.29)
or [Platform as a Service (PaaS)](https://en.wikipedia.org/wiki/Platform_as_a_service).
To do this, we need to pack the application's source code, the binary classes, its transitive dependencies,
and the configuration values into a self-contained executable file (or *fat-JAR*).
This is done with the [`sbt-assembly` plugin](https://github.com/sbt/sbt-assembly).
This plugin was originally ported from codahale's assembly-sbt, and may have been inspired by Maven's assembly plugin.
Its goal is to build a fat JAR of your project with all of its dependencies.

To execute the plugin, you should open a shell session at the project's root directory and run the following command:

    $ sbt "project api" assembly

Note that you should provide the database configuration variables to the `sbt` command, using any of the methods described above.
Otherwise, the `sbt` fails due to the `flyway` plugin.
By default, the fat jar will be created in the `{appPath}/modules/api/target/scala-2.11/` folder.

## License

Copyright (C) 2012 47 Degrees, LLC Reactive. http://47deg.com hello@47deg.com

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
