import models.{SongRepo, SchemaDefinition}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsObject, JsString, Json}
import sangria.ast.Document

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import sangria.macros._
import sangria.execution.Executor
import sangria.execution.deferred.DeferredResolver
import sangria.marshalling.playJson._

class SchemaSpec  extends AnyWordSpec with Matchers {
  "MusicService Schema" should {
      "correctly find song using its ID provided through variables" in {
        val query =
          graphql"""
         query FetchSomeIDQuery($$songId: Long!) {
           song(id: $$songId) {
             name
           }
         }
       """

        executeQuery(query, vars = Json.obj("songId" → JsString("1"))) should be (Json.parse(
          """
         {
           "data": {
             "song": {
               "name": "Yesterday"
             }
           }
         }
        """))
      }
    }

  def executeQuery(query: Document, vars: JsObject = Json.obj()) = {
    // fixme: too difficult... cached songs?
    val futureResult = Executor.execute(SchemaDefinition.schema, query,
      variables = vars,
      userContext = new SongRepo,
      deferredResolver = DeferredResolver.fetchers(SchemaDefinition.songs))
    Await.result(futureResult, 10.seconds)
  }
}