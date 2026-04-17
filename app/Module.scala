import com.google.inject.AbstractModule
import org.mongodb.scala.MongoClient

class Module extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[MongoClient]).toProvider(classOf[MongoClientProvider])
  }
}
