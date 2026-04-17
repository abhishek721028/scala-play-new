# LibraryService Explained (Detailed, Beginner Friendly)

This guide explains `app/services/LibraryService.scala` in detail, line by line, and also explains the key Scala ideas used (`Future`, `Either`, `Option`, pattern matching).

---

## First: what is this file for?

`LibraryService` is your **business logic layer**.

It sits between:
- the **controller** (HTTP layer), and
- the **repository** (MongoDB data layer).

So this file decides:
- what is valid/invalid input,
- when to return domain errors,
- when to call repository methods.

It does **not** build HTTP responses directly (`Ok`, `NotFound`, etc.). That is controller’s job.

---

## Full line-by-line walkthrough

### `L1`
- `package services`
- Puts this code in the `services` package.

### `L3`
- `import javax.inject.{Inject, Singleton}`
- Used for dependency injection:
  - `@Inject`: constructor dependencies are provided by Guice.
  - `@Singleton`: only one service instance for app lifetime.

### `L4`
- `import models.{Author, Book, CreateAuthorRequest, CreateBookRequest}`
- Imports:
  - output/domain models: `Author`, `Book`
  - request payload models: `CreateAuthorRequest`, `CreateBookRequest`

### `L5`
- `import org.bson.types.ObjectId`
- MongoDB id type. Used to validate/convert hex string ids.

### `L6`
- `import repositories.MongoLibraryRepository`
- Imports repository so service can query/insert data.

### `L8`
- `import scala.concurrent.{ExecutionContext, Future}`
- `Future` for async calls (DB operations).
- `ExecutionContext` provides thread pool for running callbacks (`map`, `flatMap`).

---

### `L10`
- `sealed trait LibraryServiceError`
- Base type for all service-level errors.
- `sealed` means all subclasses are in this file, so compiler can help with exhaustive matching.

### `L11`
- `case class InvalidObjectId(message: String) extends LibraryServiceError`
- Error when id format is invalid (or reused here for validation messages like blank fields).

### `L12`
- `case class AuthorNotFound(authorId: String) extends LibraryServiceError`
- Error when an author id is syntactically valid but no matching author exists.

### `L13`
- `case class BookNotFound(bookId: String) extends LibraryServiceError`
- Error when book id is valid format but not found in DB.

---

### `L15`
- `@Singleton`
- Single service instance.

### `L16-L18`
- Class declaration:
  - `repo: MongoLibraryRepository` injected in constructor.
  - `(implicit ec: ExecutionContext)` means every method can use `Future` transformations without passing `ec` manually each time.

---

### `L20`
- `def addAuthor(req: CreateAuthorRequest): Future[Either[LibraryServiceError, Author]] =`
- Method returns:
  - `Future[...]`: async result (because DB call is async).
  - `Either[LibraryServiceError, Author]`:
    - `Left(error)` for failure.
    - `Right(author)` for success.

This is a common functional style: represent success/failure explicitly in type.

### `L21`
- `if (req.name.isBlank) Future.successful(Left(InvalidObjectId("name must not be blank")))`
- Validation step:
  - if blank name, immediately return failed business result.
  - `Future.successful(...)` creates an already-completed future (no DB call needed).

### `L22`
- `else repo.insertAuthor(req.name.trim).map(Right(_))`
- If valid:
  - trim input name,
  - call repository insert,
  - convert returned `Author` into `Right(author)` using `.map`.

---

### `L24`
- `def addBook(req: CreateBookRequest): Future[Either[LibraryServiceError, Book]] = {`
- Add-book flow; also returns async Either.

### `L25`
- `val authorOid = parseObjectId(req.authorId)`
- Try converting author id string into `ObjectId`.
- Result type is `Option[ObjectId]`:
  - `Some(oid)` if valid,
  - `None` if invalid.

### `L26`
- `authorOid match {`
- Pattern matching on `Option`.

### `L27-L28`
- `case None => ... InvalidObjectId(...)`
- If id format is wrong, return validation error immediately.

### `L29`
- `case Some(oid) =>`
- Valid id format; continue.

### `L30`
- `repo.findAuthorById(req.authorId).flatMap {`
- Query DB to ensure author exists.
- Uses `flatMap` because inside we return another `Future`.

### `L31`
- `case None => Future.successful(Left(AuthorNotFound(req.authorId)))`
- If no author found, return domain error.

### `L32`
- `case Some(_) =>`
- Author exists; continue with title validation and insert.
- `_` means we do not need the author value itself.

### `L33-L34`
- Validate title not blank; if blank return `Left(...)`.

### `L35`
- `else repo.insertBook(req.title.trim, oid).map(Right(_))`
- Insert book with validated author `ObjectId`.
- Wrap success into `Right(book)`.

### `L36-L38`
- Close match and method blocks.

---

### `L40`
- `def booksByAuthor(authorIdHex: String): Future[Either[LibraryServiceError, Seq[Book]]] =`
- Returns all books for one author.
- Success value is `Seq[Book]`.

### `L41-L43`
- Validate `authorIdHex` format first.
- Invalid format -> `Left(InvalidObjectId(...))`.

### `L44`
- `case Some(oid) =>`
- Continue if id format valid.

### `L45`
- `repo.findAuthorById(authorIdHex).flatMap {`
- Confirm author exists.
- This design chooses to return `AuthorNotFound` if author id is unknown.

### `L46`
- Missing author -> `Left(AuthorNotFound(...))`.

### `L47`
- Existing author -> fetch books and wrap as `Right(books)`.

### `L48-L49`
- End match/method.

---

### `L51`
- `def bookById(idHex: String): Future[Either[LibraryServiceError, Book]] =`
- Returns one book by id.

### `L52-L54`
- Validate id format first.
- Invalid format -> `Left(InvalidObjectId(...))`.

### `L55`
- `case Some(_) =>`
- Id format is valid.

### `L56`
- `repo.findBookById(idHex).map {`
- Query repository; `findBookById` returns `Future[Option[Book]]`.

### `L57`
- `case None => Left(BookNotFound(idHex))`
- No book in DB for that id.

### `L58`
- `case Some(b) => Right(b)`
- Found book; success.

### `L59-L60`
- End map and match.

---

### `L62`
- `def allBooks(): Future[Seq[Book]] = repo.findAllBooks()`
- No validation needed; just pass-through call.
- Returns all books asynchronously.

---

### `L64`
- `private def parseObjectId(hex: String): Option[ObjectId] =`
- Internal helper for id parsing.
- `private` means only usable inside this service.

### `L65`
- `try Some(new ObjectId(hex))`
- Attempt to construct Mongo `ObjectId` from string.

### `L66`
- `catch { case _: IllegalArgumentException => None }`
- If parsing fails, return `None` instead of throwing exception.
- This keeps control flow functional and safe.

### `L67`
- End of class.

---

## Core Scala concepts used here

## `Option[T]`
- Represents “maybe value”.
- `Some(value)` = exists.
- `None` = missing/invalid.
- Used here for safe ObjectId parsing.

## `Either[L, R]`
- Represents one of two possibilities:
  - `Left` = error (`LibraryServiceError`)
  - `Right` = success value (`Author`, `Book`, `Seq[Book]`)
- Makes error handling explicit in types.

## `Future[T]`
- Represents a value available later (async).
- DB operations are async, so methods return `Future`.

## `map` vs `flatMap`
- `map`: transform inside a `Future` when result is plain value.
  - Example: `Future[Author]` -> `Future[Either[Err, Author]]`
- `flatMap`: chain another async call that already returns `Future[...]`.
  - Prevents nested `Future[Future[T]]`.

---

## Why this design is good

1. Validation is centralized in service layer.
2. Controller stays clean and focused on HTTP translation.
3. Repository stays clean and focused on DB only.
4. Explicit typed errors (`LibraryServiceError`) improve readability and maintainability.
5. No exceptions used for normal business flow.

---

## End-to-end example flow (`addBook`)

1. Controller parses JSON into `CreateBookRequest`.
2. Service validates `authorId` format (`parseObjectId`).
3. Service checks if author exists.
4. Service validates title non-blank.
5. Service inserts book via repository.
6. Service returns `Right(book)` or `Left(error)`.
7. Controller converts that to `201` / `400` / `404`.

---

## Quick improvement ideas (later)

- Introduce a separate `ValidationError` type instead of reusing `InvalidObjectId` for blank fields.
- Use helper methods to remove repeated `Future.successful(Left(...))` patterns.
- Add unit tests for each branch (`None`, `Some`, blank title/name, not found cases).
