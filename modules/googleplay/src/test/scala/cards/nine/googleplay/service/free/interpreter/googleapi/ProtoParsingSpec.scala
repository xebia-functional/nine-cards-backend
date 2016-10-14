package cards.nine.googleplay.service.free.interpreter.googleapi

import cards.nine.googleplay.proto.GooglePlay.ResponseWrapper
import java.io.File
import java.io.FileInputStream
import org.specs2.mutable.Specification

class ProtoParsingSpec extends Specification {

  val packageName = "air.fisherprice.com.shapesAndColors"

  "Responses from Google Play" should {

    "Be parsed into the response that Nine Cards needs using the Protobuf file" in {

      val resource = getClass.getClassLoader.getResource(packageName)
      resource != null aka s"Test protobuf response file [$packageName] must exist" must beTrue

      val responseFile = new File(resource.getFile)
      val fis = new FileInputStream(responseFile)

      val docid = ResponseWrapper.parseFrom(fis).getPayload.getDetailsResponse.getDocV2.getDocid

      fis.close

      docid must_=== packageName
    }
  }
}
