package cards.nine.services.free.interpreter.googleoauth

import cards.nine.commons.config.DummyConfig
import cards.nine.domain.oauth.ServiceAccount
import java.util.{ Iterator ⇒ JIter }
import org.scalacheck.{ Arbitrary, Gen }
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class ConverterSpec
  extends Specification
  with DummyConfig
  with ScalaCheck {

  import cards.nine.domain.ScalaCheck._
  import scala.collection.JavaConverters._

  val genServiceAccount: Gen[ServiceAccount] = {
    // We get these formats from reading one example of credentials

    // Generating the kind of values required for private keys is not trivial

    def wrapPrivateKey(key: String) = {
      val begin = "-----BEGIN PRIVATE KEY-----"
      val end = "-----END PRIVATE KEY-----"
      s"""$begin\n$key\n$end"""
    }

    val privateKeyIdGen: Gen[String] = Gen.listOfN(41, hexChar).map(_.mkString)

    val clientIdGen: Gen[String] = Gen.listOfN(22, Gen.numChar).map(_.mkString)

    for /*Gen*/ {
      clientId ← clientIdGen
      clientEmail ← emailGenerator.map(_.value)
      privateKey = test.privateKey
      privateKeyId ← privateKeyIdGen
      tokenUri ← Gen.alphaStr
      scopes ← Gen.listOf(Gen.alphaStr)
    } yield ServiceAccount(
      clientId     = clientId,
      clientEmail  = clientEmail,
      privateKey   = privateKey,
      privateKeyId = privateKeyId,
      tokenUri     = tokenUri,
      scopes       = scopes
    )

  }

  implicit val arbServiceAccount: Arbitrary[ServiceAccount] = Arbitrary(genServiceAccount)

  "toGoogleCredential" should {
    "obtain valid credentials for a service account" in
      prop { account: ServiceAccount ⇒

        val cred = Converters.toGoogleCredential(account)
        cred.getServiceAccountId() shouldEqual account.clientEmail
        cred.getServiceAccountPrivateKeyId() shouldEqual account.privateKeyId
        val scopes = cred.getServiceAccountScopes.iterator().asInstanceOf[JIter[String]].asScala.toList
        scopes should containTheSameElementsAs(account.scopes)
      }

  }

}
