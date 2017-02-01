[![TravisCI](https://travis-ci.com/47deg/nine-cards-backend.svg?token=qhZYP7DCaKxDnpZ6xzmz&branch=master)](https://travis-ci.com/47deg/nine-cards-backend/)
[![Codacy Badge](https://api.codacy.com/project/badge/grade/34b25607022243aeb44910745ac6f21b)](https://www.codacy.com)
[![Codacy Badge Coverage](https://api.codacy.com/project/badge/coverage/34b25607022243aeb44910745ac6f21b)](https://www.codacy.com)

# 9Cards Backend V2

This repository contains the source code of the Nine-Cards Back-End (NCBE) application.

NineCards is an Android Launcher that organizes the applications and data of your phone into several collections.
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
            - [Manually obtaining a TokenId](#manually-obtaining-a-tokenid)
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
and most of the functionality is provided by the [client](https://github.com/47deg/nine-cards-v2/).
The Back End is concerned with those features that involve information shared between several users.

The NCBE is organized within three modules, called `api`, `processes`, and `services`:

* The **api** module implements the HTTP server application that provides the endpoints for the application.
  The routes are built with the [spray](http://spray.io/) library, with [spray-json](http://github.com/spray/spray-json) serialization.

* The **processes** module implements the _business logic_ behind each endpoint. This business logic is implemented in a style from functional programming,
  based on the use of monadic operations.

* The **services** module provides the basic operations for reading and writing the objects of the data model in the database.
  The operations are built with [doobie](https://github.com/tpolecat/doobie), which is a functional-style wrapper around JDBC.

The processes and services follow a design inspired by the Scala libraries [scalaz](https://github.com/scalaz/scalaz)
and [cats](http://typelevel.org/cats/).

### Data Model

The data model, by which we mean the entities and relations handled by the application, is represented in the database
using the scheme definitions in [the migrations files](modules/api/src/main/resources/db/migration/).
It consists of five main classes: `User`, `Installation`, `Shared Collection`, `Package`, and `Subscription`.

A **User** represents an entity, usually a person, who owns a Google account.
* The `sessionToken` is a unique identifier of the user within the NCBE, as an alternative to the user's email.
  It is used in the communication for the communication between the NCBE and the client, to _hide_ the actual user.
* The `apiKey` is a private cryptographic key for the client, generated at signup by the NCBE.

An **Installation** represents a device (e.g. a mobile phone) of a *User*, in which NCBE has been installed.
* There is a `1:N` relation between **User** and **Installation**. Thus, every installation belongs to one user,
  and a user can own one or more installations.
* The `androidId` is a globally unique identifier of each Android device.
* The `deviceId` is used for _push_ notifications services.

A **Shared Collection** is a list of Android applications shared between a publisher and several subscribers.
The shared collection is annotated with information about who wrote it and when, a description text, some statistics about visits and downloads, etc.
Every *shared* collections is published (and afterwards modified) by exactly one user.
Thus, there is a  `1:N` relation between **user** and **collection**, which is represented in the field `userId` of the `users` table.
The field `author` only shows the name of that user. A user can be the author of several or none collections.

A **Package** represents an application that is included in a collection.
The field `packageName` represents the application as the root Java package of the application,
e.g. `com.fortysevendeg.ninecardslauncher`.

A **Subscription** is an unconstrained `M:N` relation between user and collections, where each entry
indicates that a user is subscribed to a collection.

### Authentication and Authorization

The Nine Cards Back-End Application authenticates almost all of the endpoints.


#### Client Signup in BackEnd

We describe now the process of interactions performed for a client (user and device) to sign up in the NCBE.
In essence, this process is just a third-party authentication (the third party being the NCBE) following the [OAuth 2.0 protocol](https://developers.google.com/identity/protocols/OAuth2).
It consists of an interaction between the NCBE, the **Google Account Services** (GAC), and the **Client**, which is the Ninecards Android application running from the user device.

1. **Grant Google Account Permissions.**
   The *Client* sends a request to the GAC to open an authorization token. The request carries the user's `email` and the `deviceId`,
   which uniquely identify the instance of the NineCardsClient issuing the request.
   If the request is accepted, the GAC responds with success and includes in the response a `tokenId`, which identifies a short-lived OAuth session within the GAC.

2. **Client signup in NCBE.**
   The *Client* sends a HTTP request to the NCBE's endpoint `POST {apiRoot}/login`, to register in it.
   This request carries (as a JSON object in the body entity) three fields:
   * the `email` that serves as an external identifier for the user running the client,
   * the `androidId`, that identifies the Android device in which the user is running the client application, and
   * the `tokenId` that the client received from the GAC in Step 1.

   If the request succeeds, the NCBE records the client's user and device and returns a response to the client app.
   The response carries (in a JSON object in the request body) two fields: a `sessionToken` and an `apiKey`.
   * The `sessionToken` is a unique identifier of the user within the NCBE instead of the user's email.
   * The `apiKey` is the private cryptographic key that the client uses after signup to authenticate in the NCBE.

3. **NCBE access to GAC.**
   To carry out the process of endpoint `POST {apiRoot}/login`, the NCBE communicates to the GAC to validate the `tokenId` from the request body.
   If the `tokenId` is validated, the GAC returns a successful response.

#### User Authentication

All NCBE endpoints, except from the one to read the API documentation and the one to signup,
carry out an authentication step, to check that the client app sending the requests is acting for a registered user.
The information for user authentication is carried in the HTTP headers `X-Android-ID`, `X-Auth-Token` and `X-Session-Token`.

* The `X-Android-ID` should give the `androidId` of the client's device. Note that, since the GAC process in the signup
  involves a user and device, it is the device itself and not just the user that should be signed up beforehand.

* The `X-Session-Token` should carry the user's `sessionToken` that the NCBE generated and gave to the client in Step 3 of the signup process.
  This value acts as a way to identify the user within the NCBE.

* The `X-Auth-Token` is a [Hash-based message authentication code](https://en.wikipedia.org/wiki/Hash-based_message_authentication_code), which is used for authenticating the request.
  It ensures that the client which is using the `sessionToken` is the one that is acting for that user.

The value of the `X-Auth-Token` header is computed as follows.
The *message* to be signed is just the full [URI](https://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.2.2) of the request, including protocol, host, port, path, and query.
The *cryptographic key* used is the user's `apiKey`, which the NCBE generates and gives to the client (with the `sessionToken`) at the end of the signup process.
The code is then calculated using the `sha512` hash function.
To calculate the value, you can use one of these methods:

* The command `openssl` can generate the HMAC digest of a message. For example, to digest the URI `http://localhost:8080/collections/a` with the key `foo`, you would run:

        echo -n "http://localhost:8080/collections/a" | openssl dgst -sha512 -hmac "foo" | awk '{print $2}'

* This [web page](http://www.freeformatter.com/hmac-generator.html) offers a graphic dialog to generate the digest.
  The request URL would be into the `message` dialog, the user's `apiKey` would go into the `SecretKey` text box,
  and the algorithm would be "SHA512". The result would be the digest.

#### Manually obtaining a TokenId

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
3. After pressing _Allow_, the playground page would change to a new view. The modified page shows you `Request/Response`.
   In the left pane there is a text field labelled _Authorization Code_, and a button labelled _Exchange authorization code for tokens_.
   Press this button. Google OAuth playground generates then a new token, which is shown in the API response in the right pane, in a field named as _id_token_.
   This _id_token_ identifies a session within the _Google Account Services_, which is to expire within the hour.
4. Copy the value of the _id_token_ generated. A request to login endpoint `POST {apiRoot}/login` should include in the
   body a JSON object with the field `tokenId`. The value of this field should be the _id_token_.


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

You should ensure that the `PATH` environment variable contains the directories in which the programs `scala`, `scalac`, and `sbt` are located.

### Postgres Database Setup

The NCBE stores its information in a database, using the [[postgresql]] database system.
It uses a database called `ninecards`. To write, run, and test NCBE in your machine, you can create
a user `ninecards_user`, with password `ninecards_pass`.

#### Installation

In a Debian-based Linux distribution, you can use the `apt-get` command to install the packages

    sudo apt-get install postgresql postgresql-contrib postgresql-client pgadmin3

For OS-X users, you can use any of the tools mentioned [here](http://www.postgresql.org/download/macosx/).

#### Setting Client authentication

In PostgreSQL, the "Client Authentication" method, used for opening a client's session, can be set differently for each user, database, or connection.
This configuration is kept in a file called [`pg_hba.conf`](http://www.postgresql.org/docs/9.1/static/auth-pg-hba-conf.html).

* In Debian-based distributions, it is located in the directory `/etc/postgresql/{version}/main/pg_hba.conf`.
* In OS-X, you can find it using the command `locate pg_hba.conf`, or following [these instructions](http://stackoverflow.com/questions/14025972/postgresql-how-to-find-pg-hba-conf-file-using-mac-os-x).

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

The evolutions for the data schema of the `ninecards` database are managed by `sbt`, the build system, using the [flyway SBT plugin](https://flywaydb.org/documentation/sbt/).
Flyway needs some configuration parameters to access the database.
An overview on how to pass these settings is given in the [Database Connection Configuration](#database-connection-configuration) section.
Suffice it to say that, to run the migrations on your local database, you can use the configuration values written in the [`localhost.conf`](modules/api/src/main/resources/localhost.conf) file.
You can pass this file to `sbt`, by opening a shell session in the `nine-cards-backend` root directory and executing the following command:

        sbt -Dconfig.file="modules/api/src/main/resources/localhost.conf"

This should open an interactive `sbt` session. Inside this session,
you can clear the database with the command `flywayClean`, or perform the database migrations with `flywayMigrate`.

**Note**: since `flyway` connects to the database through JDBC, you would need to configure the PostgreSQL authentication file `pg_hba.conf`, as explained [in a previous section](#setting-client-authentication)).


## Running and testing the application

From a command line, within the root directory of the project, run the following:

    $ sbt -Dconfig.file="modules/api/src/main/resources/localhost.conf"
    > project api
    > run

To check that the application has started correctly, you can check the healthcheck endpoint at the `http://localhost:8080/healthcheck` URL.

### Database Connection Configuration

The configuration is managed using Lightbend's [configuration library](https://github.com/typesafehub/config).
The default configuration is at the `modules/api/src/main/resources/application.conf` file, which
loads the values for some configuration settings from the environment. This gives you several ways to
define your configuration settings:

a. Run `sbt` passing the configuration settings, each setting having the form `-D{key}}={{value}}`.
   For example, to run the application in your local host, you would pass the databae configuration as follows:

        sbt -Ddb.default.driver="org.postgresql.Driver" -Ddb.default.url="jdbc:postgresql://localhost/ninecards" -Ddb.default.user="ninecards" -Ddb.default.password="ninecards_pass"

b. Write a configuration file with your settings, and pass that file to `sbt` using the `-Dconfig.file` option.
    For example, to run the application in yout local host, you can pass the [`localhost.conf` file](modules/api/src/main/resources/localhost.conf), as follows:

        sbt -Dconfig.file="modules/api/src/main/resources/localhost.conf"

c. Set the shell environment variables used by the default configuration file.
    In `bash`, this is done with the command `export VAR=[VALUE]`, without spaces.
    For instance, to initialize the environment variables related to the database configuration, and set them for local execution, you would run the following:

        export DB_DEFAULT_DRIVER="org.postgresql.Driver"
        export DB_DEFAULT_URL="jdbc:postgresql://localhost/ninecards"
        export DB_DEFAULT_USER="ninecards"
        export DB_DEFAULT_PASSWORD="ninecards_pass"

    Note that there should be no whitespace around the `=` sign. Note also that the settings only remain for the bash session.
    You can write such settings in the `.bashrc` file, or in a executable shell script.

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

### Running SQL evolutions in Heroku

This task should be done manually in this way:

    $ heroku pg:psql --app nine-cards < /path/to/file.sql

## License

Copyright (C) 2017 47 Degrees, LLC Reactive. http://47deg.com hello@47deg.com

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
