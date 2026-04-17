package models

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{Json, Reads, Writes, __}

case class Author(id: String, name: String)

object Author {
  implicit val writes: Writes[Author] = Json.writes[Author]
}

case class Book(id: String, title: String, authorId: String)

object Book {
  implicit val writes: Writes[Book] = Json.writes[Book]
}

case class CreateAuthorRequest(name: String)

object CreateAuthorRequest {
  implicit val reads: Reads[CreateAuthorRequest] =
    (__ \ "name").read[String].map(CreateAuthorRequest.apply)
}

case class CreateBookRequest(title: String, authorId: String)

object CreateBookRequest {
  implicit val reads: Reads[CreateBookRequest] =
    (
      (__ \ "title").read[String] and
        (__ \ "authorId").read[String]
    )(CreateBookRequest.apply _)
}

case class ErrorResponse(error: String)

object ErrorResponse {
  implicit val writes: Writes[ErrorResponse] = Json.writes[ErrorResponse]
}
