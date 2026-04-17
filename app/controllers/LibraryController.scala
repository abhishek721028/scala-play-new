package controllers

import javax.inject.{Inject, Singleton}
import models.{CreateAuthorRequest, CreateBookRequest, ErrorResponse}
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.ExecutionContext
import services.{
  AuthorNotFound,
  BookNotFound,
  InvalidObjectId,
  LibraryService,
  LibraryServiceError
}

@Singleton
class LibraryController @Inject() (
    val controllerComponents: ControllerComponents,
    libraryService: LibraryService
)(implicit ec: ExecutionContext)
    extends BaseController {

  def index: Action[AnyContent] = Action {
    Ok(
      Json.obj(
        "service" -> "library",
        "endpoints" -> Json.arr(
          "POST /api/authors",
          "POST /api/books",
          "GET /api/authors/:authorId/books",
          "GET /api/books/:id",
          "GET /api/books"
        )
      )
    )
  }

  def addAuthor: Action[CreateAuthorRequest] =
    Action.async(parse.json[CreateAuthorRequest]) { request =>
      libraryService.addAuthor(request.body).map {
        case Right(author) => Created(Json.toJson(author))
        case Left(err)     => badRequest(err)
      }
    }

  def addBook: Action[CreateBookRequest] =
    Action.async(parse.json[CreateBookRequest]) { request =>
      libraryService.addBook(request.body).map {
        case Right(book) => Created(Json.toJson(book))
        case Left(AuthorNotFound(id)) =>
          NotFound(Json.toJson(ErrorResponse(s"author not found: $id")))
        case Left(err) => badRequest(err)
      }
    }

  def booksByAuthor(authorId: String): Action[AnyContent] = Action.async {
    libraryService.booksByAuthor(authorId).map {
      case Right(books) => Ok(Json.toJson(books))
      case Left(AuthorNotFound(id)) =>
        NotFound(Json.toJson(ErrorResponse(s"author not found: $id")))
      case Left(err) => badRequest(err)
    }
  }

  def bookById(id: String): Action[AnyContent] = Action.async {
    libraryService.bookById(id).map {
      case Right(book) => Ok(Json.toJson(book))
      case Left(BookNotFound(bookId)) =>
        NotFound(Json.toJson(ErrorResponse(s"book not found: $bookId")))
      case Left(err) => badRequest(err)
    }
  }

  def allBooks: Action[AnyContent] = Action.async {
    libraryService.allBooks().map(books => Ok(Json.toJson(books)))
  }

  private def badRequest(err: LibraryServiceError): Result =
    err match {
      case InvalidObjectId(msg) =>
        Results.BadRequest(Json.toJson(ErrorResponse(msg)))
      case AuthorNotFound(id) =>
        NotFound(Json.toJson(ErrorResponse(s"author not found: $id")))
      case BookNotFound(id) =>
        NotFound(Json.toJson(ErrorResponse(s"book not found: $id")))
    }
}
