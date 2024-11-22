---
date: 2023-03-30
---

> How `Http4s` leverages Scala’s powerful pattern-matching features, and how you can use them too.
>
<!-- truncate -->

I’ve recently played around with `Http4s`’s routing DSL and added some custom mechanics of my own.
Sometimes we want to access request information generically independent if the route’s normal purpose. `Http4s`'
`AuthedRoutes` serves as a great example for this.

```mdoc:scala
val normal: HttpRoutes[IO] = HttpRoutes.of {
  case GET -> / "user" / UserId(user) / "resource" 
}

val authed: AuthedRoutes[UserId, IO] = AuthedRoutes.of {
  case GET -> / "user" / "resource" as user
}
```

The second route shows an example of using some new DSL syntax. That small `as user` at the end of the second route
definition does a lot of heavy lifting around user authentication. Even better as it modifies the route type itself we
can't forget to add authentication to a route (which we might if added auth route by route).

For my change I will focus on a relatively neutral change that works with the normal `Http4s` request and response
types. Let’s work with a simple requirement.

> Modify the route’s behaviour based on the `User-Agent` of the caller.

How might that look? Let’s sketch out a possible DSL.

```mdoc:scala
HttpRoutes.of[IO] {
  case Service.Distributor using GET -> Root / "resource" / IntVar(id) =>
}
```

To understand how we might add this we need to explore how `Http4s` cleverly uses Scala’s pattern match system. When we
pattern match on a case class Scala invokes a method called `unapply` to decide if the given value “matches”. Typically,
that method might look something like this.

```mdoc:scala
object Foo {
  def unapply(foo: Foo): Option[Int]
}
```

In this case the implementation knows how to decompose a `Foo` into a `Int`, by using the class' `foo` field. Sometimes
we can't access a field like that (for example, an ADT) and so the optional return type allows us to indicate the match
failed. This method doesn't have to be synthetic and Scala allows us to define our own custom `unapply` matchers, which
Scala refers to as [extractor objects](https://docs.scala-lang.org/tour/extractor-objects.html).

`Http4s` itself has some great examples of when we might want to use extractors. The status matcher `Succesful` matches
response statuses within the 2xx range, with similar matchers for 4xx and 5xx too. This showcases the first style of
matcher — a way of grouping similar terms in a match.

You can nest Scala’s matchers in the same way that you can nest data. For example;

```mdoc:scala
  case Right(Some(3)) =>
   //   ^     ^
   //   |     |_ Then it applies the `Option` matcher to the result.
   //   |
   //   |_ First applies the `Either` matcher
```

This nesting also powers the familiar list matcher.

```mdoc:scala
  case 1 :: 2 :: Nil =>
  // really looks like
  case ::(1, ::(2, ::(3, ::(Nil))) => 
```

This syntax eliminates the normal bracket syntax see in favour of something more readable. Seeing this you might see how
the `Http4s` DSL takes shape. Using DSL objects (made available via the `Htp4sDSL`) trait we can string a series of
matchers along to define our expected request structure. Checking the source code gives us an idea on how to structure
our own custom matcher.

The first component of a `Http4s` route:

```mdoc:scala
object -> {

  /** HttpMethod extractor:
    * {{{
    *   (request.method, Path(request.path)) match {
    *     case Method.GET -> Root / "test.json" => ...
    * }}}
    */
  def unapply[F[_]](req: Request[F]): Some[(Method, Path)] = ...
}
```

It takes as input the inbound request itself and outputs two chunks split from the request; the method and the path.
Through the same syntax as `::` that we saw earlier; Scala can match the request as a method on the left and a path on
the
right. Scala also allows us to match against specific values, so we can further refine our match with an exact method.
The path however gets fed to another `Path` matcher, which decomposes the value further.

```modc:scala
object / {
  def unapply(path: Path): Option[(Path, String)] = ...
}
```

This path matcher consumes a `Path` and splits to a `String` component on its right-hand side. The left-hand side
returns another `Path` so we can decompose the path string as much as we need to.

You should see a pattern emerging, and we can start implementing our own. Let’s revisit our desired syntax.

```mdoc:scala
HttpRoutes.of[IO] {
  case Service.Distributor using GET -> Root / "resource" / IntVar(id) =>
}
```

We can start building our matcher by looking at our preferred inputs and outputs. In this case we will need:

```mdoc:scala
Input:  Request[IO]
Output: (Service , Request[IO])
```

It takes a request and returns it plus a newly created `Service` object. Returning the request enables us to continue
matching on it down the line. A trivial implementation looks a little like this;

<script src="https://scastie.scala-lang.org/s5swyEcORHGz0wYU67AZ4w.js"></script>

> Notice the `unapply` method actually has a generic type parameter. Pattern matches do not support type parameters so
> if you include them they must be inferred from the matcher input.

The logic here could become more complex if we wanted it to, for example we could extract a specific user-agent version
too. Like `AuthedRoutes` the best syntax additions are obvious and limited. By changing the order of the outputs we
could move our syntax the end of the match rather than the front. Experimenting with the values you extract in your
match brings up some interesting possibilities. Take `Http4s`' `->>` matcher as an example.

```modc:scala
  /** Extractor to match an http resource and then enumerate all supported methods:
    * {{{
    *   (request.method, Path(request.path)) match {
    *     case withMethod ->> Root / "test.json" => withMethod {
    *       case Method.GET => ...
    *       case Method.POST => ...
    * }}}
    *
    * Returns an error response if the method is not matched, in accordance with [[https://datatracker.ietf.org/doc/html/rfc7231#section-4.1 RFC7231]]
    */
  def unapply[F[_]: Applicative](
      req: Request[F]
  ): Some[(PartialFunction[Method, F[Response[F]]] => F[Response[F]], Path)] = ...
```

Returning a partial-function yields control back to the caller, who can add their own custom behaviour. This allows for
some powerful DSLs that can make defining request-dependent service behaviour a breeze.

The big question is… _should you?_ This technique can help simplify a lot of code, but it can make
it worse too. Common drawbacks and criticisms of custom DSLs include;

* Confusing or pointlessly obtuse syntax (for example, `|@|` from cats).
* They can hide too much. Subtle behaviours introduced through syntax can feel “magic”.
* Newcomers need to learn your syntax on top of a language they might not know already.

You and your team decide how far you want to take it. A part of the art of software development includes deciding when
to use these kinds of techniques.

> “Perfection is achieved, not when there is nothing more to add, but when there is nothing left to take away.”
> ― Antoine de Saint-Exupéry

Even small uses can benefit from custom matchers, for example this…

```mdoc:scala
  case Left(Some(event)) if event.timestamp > cutOffTime =>
```

…into this…

```mdoc:scala
  case LiveEvent(event) =>
```

Which reduces cramped matchers into something simple and self-documenting.
