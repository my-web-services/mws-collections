package meta

import macros.Model
import play.api.libs.json.Json
import play.api.test.PlaySpecification
import slick.model.Table

class MetaSpec extends PlaySpecification {

  @Model
  case class MyTest(int: Int, str: String)

  val testee = MyTest(4, "four", "orestis")

  "Macro annotations" should {

    "augment with account" in {
      testee.accountId === "orestis"
    }

    "define implicit conversion method" in {
      Json.toJson(testee) === Json.obj(
        "int" -> 4, "str" -> "four", "accountId" -> "orestis"
      )
    }

    "extend to Slick table definition" in {
      testee.isInstanceOf[Table] === true
    }
  }
}
