
# Nine Cards Back End Architecture

This file describes the architecture of the Nine Cards Backend.

## Overview

The backend source code is organised into four modules, or `sbt` projects, called
`api`, `processes`, `services`, `googleplay`, and `commons`.

* The [`commons`](/modules/commons) module contains the definition of the backend's `domain` classes,
  and some utility method for common libraries, such as `cats` or `scalaz`.
* The [`api`](/modules/api) module implements the `REST-HTTP` application's Api, and the application's
  main method.
* The [`processes`](/modules/processes) module contains the application's process methods. Each process methods implements
  the functionality of one endpoint, using one or more services.
* The [`services`](/modules/services) module implements the backend's services. Each service interacts either
  with a particular section of the application' database, or with the `redis` cache, or with a remote api.

In what follows we explain each layer with more detail.

### External Systems

The backend

#### Libraries

The backend depends for its functionality on several external libraries, apart from the `Scala` libraries.
The main libraries and frameworks used in the backend are the following ones:

* [Cats](http://typelevel.org/cats/) is a core library for the backend. The architecture follows the
  [Data Types à la carte](http://dblp.org/rec/html/journals/jfp/Swierstra08) article, and to implement
  such architecture we make use of `cats` implementations for [`Monad`](http://github.com/typelevel/cats/blob/master/core/src/main/scala/cats/Monad.scala),
  for [free monads](http://github.com/typelevel/cats/blob/master/free/src/main/scala/cats/free/Free.scala),
  [natural transformations]([`cats.arrow.FunctionK`](http://github.com/typelevel/cats/blob/master/core/src/main/scala/cats/arrow/FunctionK.scala),
  and in a few cases we also use some [monad transformers](http://github.com/typelevel/cats/blob/master/core/src/main/scala/cats/data/EitherT.scala) from

* [Spray](http://spray.io/) is used to build the `HTTP-REST` API, that serves as the external
  interface for the backend application. This `api` is used by each Nine Cards client to access
  the functionality of the backend. The entities transmitted through this API are all encoded in
  [Json](http://en.wikipedia.org/wiki/JSON), for which we use `spray-json`.
* [Circe](http://travisbrown.github.io/circe/), a library for implementing JSON encoding and decoding for
  data classes. Circe is used for a few classes in implementing the `api`, but it is mostly used for the
  communication with the external HTTP-REST services.
* [Akka](http://akka.io/) is used, but just enough to support the `spray` api.
* [Doobie](http://github.com/tpolecat/doobie) is used for the communication with the database.

### Services Layer

The services layer deals with the methods that interact with other systems outside the backend,
either to bring information into the application or out of it, or to execute external actions.


#### Algebra - Interpreter Separation

The design of each service follows a separation between operation algebra and interpretation,
inspired by the [Data Types à la carte](http://dblp.org/rec/html/journals/jfp/Swierstra08) article.

The **algebra** of a service defines an abstract data type `Ops[A]`, which represent the possible
operations that can be done in that service.
Each operation can be seen as a [command](http://en.wikipedia.org/wiki/Command_pattern) object,
which carries the input parameters needed to specify the value of the result.
As a guideline, each operation in an algebra should be atomic, and involve at most one interaction
with the external system in question.

The **interpreter** is the part of the service that takes the operations of the algebra and computes the results of that operation.
An `Interpreter` is a (class of) object that implements a function from the service's algebra ADT
`Ops[A]` to another parametric type, `F[A]`, with the same base type `A`.
Operations that transform from one parametric type `F[A]` to another parametric type `G[A]` are called _natural transformations_;
and in the backend every service interpreters must be a subtype of [`cats.arrow.FunctionK`](http://github.com/typelevel/cats/blob/master/core/src/main/scala/cats/arrow/FunctionK.scala).
The target type `F` can be `Task`](http://github.com/scalaz/scalaz/blob/series/7.3.x/concurrent/src/main/scala/scalaz/concurrent/Task.scala),
to represent an asynchronous computation of the result,
or `ConnectionIO`, to represent a computation that performs queries with [`doobie`](http://github.com/tpolecat/doobie/blob/master/core/src/main/scala/doobie/free/connection.scala)
or just `Id`, to represent a result obtained just within the application (for testing).

There are several services in the `services` layer.

####


### Endpoints

This module contains some utility methods shared

Most of the user's data and configuration (such as her collections) is kept in her Google Drive account,
and most of the functionality is provided by the [client](http://github.com/47deg/nine-cards-v2/).
The Back End is concerned with those features that involve information shared between several users.

The NCBE is organized within three modules, called `api`, `processes`, and `services`:

* The **api** module implements the HTTP server application that provides the endpoints for the application.
  The routes are built with the [spray](http://spray.io/) library, with [spray-json](http://github.com/spray/spray-json) serialization.

* The **processes** module implements the _business logic_ behind each endpoint. This business logic is implemented in a style from functional programming,
  based on the use of monadic operations.

* The **services** module provides the basic operations for reading and writing the objects of the data model in the database.
  The operations are built with [doobie](http://github.com/tpolecat/doobie), which is a functional-style wrapper around JDBC.

The processes and services follow a design inspired by the Scala libraries [scalaz](http://github.com/scalaz/scalaz)
and [cats](http://typelevel.org/cats/).

