package com.fortysevendeg.ninecards.services.utils

import cats.data.Xor
import org.specs2.matcher.{OptionLikeCheckedMatcher, OptionLikeMatcher, ValueCheck}

trait XorMatchers {

  def beXorRight[T](t: ValueCheck[T]) = RightXorCheckedMatcher(t)
  def beXorRight[T] = RightXorMatcher[T]()

  def beXorLeft[T](t: ValueCheck[T]) = LeftXorCheckedMatcher(t)
  def beXorLeft[T] = LeftXorMatcher[T]()

}

object XorMatchers extends XorMatchers

case class RightXorMatcher[T]() extends OptionLikeMatcher[({type l[a]= _ Xor a})#l, T, T]("\\/-", (_:Any Xor T).toEither.right.toOption)
case class RightXorCheckedMatcher[T](check: ValueCheck[T]) extends OptionLikeCheckedMatcher[({type l[a]= _ Xor a})#l, T, T]("\\/-", (_:Any Xor T).toEither.right.toOption, check)

case class LeftXorMatcher[T]() extends OptionLikeMatcher[({type l[a]= a Xor _})#l, T, T]("-\\/", (_:T Xor Any).toEither.left.toOption)
case class LeftXorCheckedMatcher[T](check: ValueCheck[T]) extends OptionLikeCheckedMatcher[({type l[a]=a Xor _})#l, T, T]("-\\/", (_: T Xor Any).toEither.left.toOption, check)

