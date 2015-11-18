package com.fortysevendeg.ninecards.free.domain

import org.joda.time.DateTime


case class SharedCollectionList(items: Seq[SharedCollection])

case class SharedCollection(
  _id: Option[String] = None,
  sharedCollectionId: Option[String],
  publishedOn: Option[DateTime] = None,
  description: Option[String] = None,
  screenshots: Seq[AssetResponse] = Nil,
  author: Option[String] = None,
  tags: Seq[String] = Nil,
  name: String,
  shareLink: Option[String] = None,
  packages: Seq[String] = Nil,
  resolvedPackages: Seq[SharedCollectionPackage],
  occurrence: Seq[UserConfigTimeSlot] = Nil,
  lat: Option[Double] = None,
  lng: Option[Double] = None,
  alt: Option[Double] = None,
  views: Option[Int] = None,
  category: Option[String] = None,
  icon: Option[String] = None,
  community: Boolean = false)

case class SharedCollectionPackage(
  packageName: String,
  title: String,
  description: String,
  icon: String,
  stars: Double,
  downloads: String,
  free: Boolean)

case class SharedCollectionSubscription(
  _id: Option[String] = None,
  sharedCollectionId: String,
  userId: String)

case class AssetResponse(
  uri: String,
  title: Option[String] = None,
  description: Option[String] = None,
  contentType: Option[String] = None,
  thumbs: Seq[AssetThumbResponse] = Nil)

case class AssetThumbResponse(
  url: String,
  width: Option[Int] = None,
  height: Option[Int] = None,
  `type`: Option[String] = None)