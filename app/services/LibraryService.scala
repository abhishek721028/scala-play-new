package services

import javax.inject.{Inject, Singleton}
import models.{Author, Book, CreateAuthorRequest, CreateBookRequest}
import org.bson.types.ObjectId
import repositories.MongoLibraryRepository

import scala.concurrent.{ExecutionContext, Future}

sealed trait LibraryServiceError
case class InvalidObjectId(message: String) extends LibraryServiceError
case class AuthorNotFound(authorId: String) extends LibraryServiceError
case class BookNotFound(bookId: String) extends LibraryServiceError

@Singleton
class LibraryService @Inject() (repo: MongoLibraryRepository)(implicit
    ec: ExecutionContext
) {

  def addAuthor(req: CreateAuthorRequest): Future[Either[LibraryServiceError, Author]] =
    if (req.name.isBlank) Future.successful(Left(InvalidObjectId("name must not be blank")))
    else repo.insertAuthor(req.name.trim).map(Right(_))

  def addBook(req: CreateBookRequest): Future[Either[LibraryServiceError, Book]] = {
    val authorOid = parseObjectId(req.authorId)
    authorOid match {
      case None =>
        Future.successful(Left(InvalidObjectId("authorId must be a valid ObjectId hex string")))
      case Some(oid) =>
        repo.findAuthorById(req.authorId).flatMap {
          case None => Future.successful(Left(AuthorNotFound(req.authorId)))
          case Some(_) =>
            if (req.title.isBlank)
              Future.successful(Left(InvalidObjectId("title must not be blank")))
            else repo.insertBook(req.title.trim, oid).map(Right(_))
        }
    }
  }

  def booksByAuthor(authorIdHex: String): Future[Either[LibraryServiceError, Seq[Book]]] =
    parseObjectId(authorIdHex) match {
      case None =>
        Future.successful(Left(InvalidObjectId("authorId must be a valid ObjectId hex string")))
      case Some(oid) =>
        repo.findAuthorById(authorIdHex).flatMap {
          case None => Future.successful(Left(AuthorNotFound(authorIdHex)))
          case Some(_) => repo.findBooksByAuthor(oid).map(Right(_))
        }
    }

  def bookById(idHex: String): Future[Either[LibraryServiceError, Book]] =
    parseObjectId(idHex) match {
      case None =>
        Future.successful(Left(InvalidObjectId("id must be a valid ObjectId hex string")))
      case Some(_) =>
        repo.findBookById(idHex).map {
          case None    => Left(BookNotFound(idHex))
          case Some(b) => Right(b)
        }
    }

  def allBooks(): Future[Seq[Book]] = repo.findAllBooks()

  private def parseObjectId(hex: String): Option[ObjectId] =
    try Some(new ObjectId(hex))
    catch { case _: IllegalArgumentException => None }
}
