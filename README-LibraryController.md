# LibraryController Explained (Beginner Friendly)

This document explains `app/controllers/LibraryController.scala` line by line.

## Full file with explanations

### `L1`
- `package controllers`
- This tells Scala that this class belongs to the `controllers` package.

### `L3`
- `import javax.inject.{Inject, Singleton}`
- Brings in annotations for dependency injection:
  - `@Inject` lets Play/Guice provide dependencies automatically.
  - `@Singleton` makes sure only one instance of this controller is created.

### `L4`
- `import models.{CreateAuthorRequest, CreateBookRequest, ErrorResponse}`
- Imports request/response model classes used for JSON input/output.

### `L5`
- `import play.api.libs.json.Json`
- Imports Play JSON helpers (`Json.obj`, `Json.arr`, `Json.toJson`).

### `L6`
- `import play.api.mvc._`
- Imports Play MVC types like `Action`, `Result`, `AnyContent`, `ControllerComponents`, `BaseController`.

### `L8`
- `import scala.concurrent.ExecutionContext`
- Needed for asynchronous code (`Future.map` etc.).

### `L9-L15`
- Imports error types and service class from the `services` package:
  - `AuthorNotFound`
  - `BookNotFound`
  - `InvalidObjectId`
  - `LibraryService`
  - `LibraryServiceError`

### `L17`
- `@Singleton`
- Marks this controller as singleton-scoped in dependency injection.

### `L18-L22`
- Class declaration and constructor injection:
  - `controllerComponents` is required by Play controllers.
  - `libraryService` is business logic layer.
  - `(implicit ec: ExecutionContext)` provides thread pool context for async operations.
  - `extends BaseController` gives helper methods like `Ok`, `Created`, `NotFound`.

### `L24`
- `def index: Action[AnyContent] = Action {`
- Defines a synchronous endpoint/action that accepts any content type.

### `L25-L37`
- Returns `200 OK` with a JSON object containing:
  - service name
  - list of available endpoints

---

### `L39`
- `def addAuthor: Action[CreateAuthorRequest] =`
- Action expects JSON body that can be parsed into `CreateAuthorRequest`.

### `L40`
- `Action.async(parse.json[CreateAuthorRequest]) { request =>`
- `parse.json[...]` validates/parses request body as JSON into model.
- `async` means the action returns a `Future[Result]`.

### `L41`
- Calls service method `addAuthor` with parsed body.

### `L42`
- `case Right(author) => Created(Json.toJson(author))`
- On success, return `201 Created` and author JSON.

### `L43`
- `case Left(err) => badRequest(err)`
- On failure, delegate to common error handler method.

---

### `L47`
- `def addBook: Action[CreateBookRequest] =`
- Action expects JSON body as `CreateBookRequest`.

### `L48-L49`
- Parse JSON and call service `addBook`.

### `L50`
- Success case: `201 Created` with book JSON.

### `L51-L52`
- Specific error handling:
  - if author does not exist, return `404 Not Found` with message.

### `L53`
- Other errors handled by `badRequest(err)`.

---

### `L57`
- `def booksByAuthor(authorId: String): Action[AnyContent] = Action.async {`
- Endpoint takes `authorId` from URL path parameter.

### `L58`
- Calls service to fetch all books for that author.

### `L59`
- Success: return `200 OK` with list of books as JSON array.

### `L60-L61`
- If author missing: return `404 Not Found`.

### `L62`
- Remaining errors -> `badRequest`.

---

### `L66`
- `def bookById(id: String): Action[AnyContent] = Action.async {`
- Endpoint takes book id from URL.

### `L67`
- Calls service `bookById`.

### `L68`
- Success: `200 OK` with single book JSON.

### `L69-L70`
- If book not found: `404 Not Found`.

### `L71`
- Other failures -> `badRequest`.

---

### `L75`
- `def allBooks: Action[AnyContent] = Action.async {`
- Endpoint to fetch all books.

### `L76`
- Calls service and returns `200 OK` with JSON array.

---

### `L79`
- `private def badRequest(err: LibraryServiceError): Result =`
- Private helper function to map domain/service errors into HTTP responses.

### `L80`
- Pattern matching starts (`err match`).

### `L81-L82`
- For `InvalidObjectId(msg)` return `400 Bad Request` and JSON error.

### `L83-L84`
- For `AuthorNotFound(id)` return `404 Not Found`.

### `L85-L86`
- For `BookNotFound(id)` return `404 Not Found`.

### `L88`
- End of class.

---

## Big-picture summary

`LibraryController` is the HTTP layer. It does three main jobs:

1. Parse incoming HTTP/JSON requests.
2. Call `LibraryService` for business logic.
3. Convert service results/errors into HTTP responses (`200`, `201`, `400`, `404`) with JSON bodies.

This keeps controller code clean and pushes real business logic into `LibraryService`.
