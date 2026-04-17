import com.google.inject.Provider
import javax.inject.{Inject, Singleton}
import org.mongodb.scala.MongoClient
import play.api.Configuration
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

@Singleton
class MongoClientProvider @Inject() (
    config: Configuration,
    lifecycle: ApplicationLifecycle
) extends Provider[MongoClient] {

  private val client: MongoClient = MongoClient(config.get[String]("mongodb.uri"))

  lifecycle.addStopHook { () =>
    client.close()
    Future.successful(())
  }

  override def get(): MongoClient = client
}
