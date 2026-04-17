package repositories

import javax.inject.{Inject, Singleton}
import models.{Author, Book}
import org.bson.types.ObjectId
import org.mongodb.scala.MongoClient
import org.mongodb.scala.bson.Document
import org.mongodb.scala.model.Filters.equal
import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MongoLibraryRepository @Inject() (
    client: MongoClient,
    config: Configuration
)(implicit ec: ExecutionContext) {

  private val db = client.getDatabase(config.get[String]("mongodb.database"))
  private val authors = db.getCollection("authors")
  private val books = db.getCollection("books")

  def insertAuthor(name: String): Future[Author] = {
    val doc = Document("name" -> name)
    authors
      .insertOne(doc)
      .head()
      .map { result =>
        val id = result.getInsertedId.asObjectId().getValue.toHexString
        Author(id, name)
      }
  }

  def findAuthorById(idHex: String): Future[Option[Author]] =
    parseObjectId(idHex).fold(Future.successful(Option.empty[Author])) { oid =>
      authors
        .find(equal("_id", oid))
        .headOption()
        .map(_.map(docToAuthor))
    }

  def insertBook(title: String, authorId: ObjectId): Future[Book] = {
    val doc = Document("title" -> title, "authorId" -> authorId)
    books
      .insertOne(doc)
      .head()
      .map { result =>
        val id = result.getInsertedId.asObjectId().getValue.toHexString
        Book(id, title, authorId.toHexString)
      }
  }

  def findBooksByAuthor(authorId: ObjectId): Future[Seq[Book]] =
    books
      .find(equal("authorId", authorId))
      .foldLeft(Seq.empty[Book])((acc, doc) => acc :+ docToBook(doc))
      .head()

  def findBookById(idHex: String): Future[Option[Book]] =
    parseObjectId(idHex).fold(Future.successful(Option.empty[Book])) { oid =>
      books
        .find(equal("_id", oid))
        .headOption()
        .map(_.map(docToBook))
    }

  def findAllBooks(): Future[Seq[Book]] =
    books
      .find()
      .foldLeft(Seq.empty[Book])((acc, doc) => acc :+ docToBook(doc))
      .head()

  private def docToAuthor(doc: Document): Author = {
    val id = doc.getObjectId("_id").toHexString
    val name = doc.getString("name")
    Author(id, name)
  }

  private def docToBook(doc: Document): Book = {
    val id = doc.getObjectId("_id").toHexString
    val title = doc.getString("title")
    val authorId = doc.getObjectId("authorId").toHexString
    Book(id, title, authorId)
  }

  private def parseObjectId(hex: String): Option[ObjectId] =
    try Some(new ObjectId(hex))
    catch { case _: IllegalArgumentException => None }
}
