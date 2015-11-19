package com.fortysevendeg.ninecards.services.free.domain

import spray.json.JsObject

case class UserConfig(
  _id: Option[String] = None,
  emailCampaigns: Option[Boolean] = None,
  plusProfile: Option[UserConfigPlusProfile] = None,
  email: String,
  devices: Seq[UserConfigDevice] = Nil,
  geoInfo: Option[UserConfigGeoInfo] = None,
  status: Option[UserConfigStatusInfo] = None)

//TODO:This case class should be implemented in the module of GooglePlay. (Now is implementing in Appsly-Server-Apps-Markets-Proxy  generated.models.GooglePlay)
case class UserConfigPlusProfile(
  displayName: Option[String] = None,
  profileImage: Option[UserConfigProfileImage] = None)

//TODO:This case class should be implemented in the module of GooglePlay. (Now is implementing in Appsly-Server-Apps-Markets-Proxy  generated.models.GooglePlay)
case class UserConfigProfileImage(
  imageType: Option[Int] = None,
  imageUrl: Option[String] = None,
  secureUrl: Option[String] = None)

case class UserConfigDevice(
  deviceId: String,
  deviceName: String,
  collections: Seq[UserConfigCollection] = Nil)

case class UserConfigGeoInfo(
  homeMorning: Option[UserConfigUserLocation] = None,
  homeNight: Option[UserConfigUserLocation] = None,
  work: Option[UserConfigUserLocation] = None,
  current: Option[UserConfigUserLocation] = None)

case class UserConfigUserLocation(
  wifi: Option[String] = None,
  lat: Option[Double] = None,
  lng: Option[Double] = None,
  occurrence: Seq[UserConfigTimeSlot] = Nil)

case class UserConfigStatusInfo(
  products: Seq[String] = Nil,
  friendsReferred: Option[Int] = Some(0),
  themesShared: Option[Int] = Some(0),
  collectionsShared: Option[Int] = Some(0),
  customCollections: Option[Int] = Some(0),
  earlyAdopter: Option[Boolean] = Some(false),
  communityMember: Option[Boolean] = Some(false),
  joinedThrough: Option[String] = None,
  tester: Option[Boolean] = Some(false))

case class UserConfigCollection(
  name: String,
  items: Seq[UserConfigCollectionItem] = Nil,
  collectionType: String,
  constrains: Seq[String] = Nil,
  wifi: Seq[String] = Nil,
  occurrence: Seq[UserConfigTimeSlot] = Nil,
  icon: Option[String] = None,
  radius: Option[Int] = None,
  lat: Option[Double] = None,
  lng: Option[Double] = None,
  alt: Option[Double] = None,
  category: Option[String] = None,
  originalSharedCollectionId: Option[String] = None,
  sharedCollectionId: Option[String] = None,
  sharedCollectionSubscribed: Option[Boolean])

case class UserConfigCollectionItem(
  itemType: String,
  title: String,
  icon: Option[String],
  metadata: Option[JsObject],
  labels: Option[Map[String, String]] = None,
  categories: Option[Seq[String]] = None)

case class UserConfigTimeSlot(
  from: Option[String] = None,
  to: Option[String] = None,
  days: Seq[Int] = Nil)

