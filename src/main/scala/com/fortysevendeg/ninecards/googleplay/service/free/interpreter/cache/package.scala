package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import com.redis.RedisClient

package object cache {

  type CacheEntry = (CacheKey, CacheVal)

  type WithClient[+A] = RedisClient => A 

}