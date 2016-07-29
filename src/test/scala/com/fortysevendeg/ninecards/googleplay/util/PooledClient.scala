package com.fortysevendeg.ninecards.googleplay.util

import org.specs2.specification.AfterAll
import org.http4s.client.blaze.PooledHttp1Client

trait WithHttp1Client extends AfterAll {

  protected[this] final val pooledClient = PooledHttp1Client()

  override def afterAll: Unit = pooledClient.shutdownNow

}

