package cards.nine.services.free.interpreter.googleoauth

import cards.nine.commons.config.DummyConfig
import cards.nine.domain.oauth.ServiceAccount
import java.util.{ Iterator ⇒ JIter }
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class ConverterSpec
  extends Specification
  with DummyConfig
  with ScalaCheck {

  import cards.nine.domain.ScalaCheck.arbServiceAccount
  import scala.collection.JavaConverters._

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
