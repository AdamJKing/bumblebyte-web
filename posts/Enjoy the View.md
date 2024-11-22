---
date: 2024-08-20
---

> Aimed at relatively new Scala developers or those unfamiliar with Scala collection performance outside the usual `List` or `Seq` types.
<!-- truncate -->

Most of the common collection types in Scala are implemented strictly; this means that at all times every element of the collection is calculated and stored in memory. This can have performance impacts if our collections come from expensive operations, contain a high number of objects, or contain particularly large objects. Some methods such as `find` or `head` don’t need to read every element of the collection and so using strict collections isn't always necessary. A good example of a collection that benefits from being lazy is the linked list (Scala's `List` type). Linked lists are implemented as an element and a pointer to the next element, instead we can make the collection lazy by storing a function for the next pointer instead. See the example below for how this works.

```
strictList = 1 -> 2 -> 3 -> 4 -> 5
lazyList   = 1 -> (n => n + 1)
```

Lists in Scala are not lazy by default and so if we want this behaviour we'll actually need to use `List`'s sister class `LazyList`. This type works a lot like a list but instead the list is stored as an element and a "thunk", a function stored on the heap which can be used to calculate the next element. Now when we use our `find` or `head` operations we only calculate as many elements as we need rather than creating elements unnecessarily. It also opens up some interesting algorithm designs that make use of infinite lists. For example, implementing a `zipWithIndex` function would look like this.

```scala
scala> LazyList.range(0, Int.MaxValue).zip("a cool example").toList
val res0: List[(Int, Char)] = List((0,a), (1, ), (2,c), (3,o), (4,o), (5,l), (6, ), (7,e), (8,x), (9,a), (10,m), (11,p), (12,l), (13,e))
```

Notice how the initial `LazyList.range(0, Int.MaxValue)` goes all the way to the maximum integer. If you replaced the `LazyList` with a regular strict `List` you’ll find the program takes a lot longer to complete (if it even does at all).

> Lazy lists actually retain their elements after they're calculated so an infinitely sized `LazyList` can still risk overrunning your heap memory! If you want to discard elements you'll need to work recursively (with tail call optimization)

What if we're not working with a lazy collection? We can actually work with any collection lazily by using a `View`. You can summon a view for a collection by using `.view` and continue to use the familiar collection traversal methods. The upside is that every operation we do will be evaluated lazily just like the lazy list! 

This example shows how the order of operations differs between a strict list and a lazy view on the same collection. 

```scala
val normalList = List.range(0, 2)
val viewedList = normalList.view

val tappedList = normalList.tapEach(_ => println("Operation A")).tapEach(_ => println("Operation B"))
val tappedView = viewedList.tapEach(_ => println("Operation A")).tapEach(_ => println("Operation B"))

println("Not done yet?")
// realise the view
tappedView.toList
```

Run this snippet and you'll see something interesting.

```
Operation A
Operation A
Operation B
Operation B
Not done yet?
Operation A
Operation B
Operation A
Operation B
```

The strict list evaluates the entire list first for the first `tapEach`, and then evaluates it again for the second `tapEach` (meaning we've actually crossed the whole list twice). In the view however the operations are actually combined making our update more efficient. Similarly we actually don't see the output of the view until we force it back into a list later on (sometimes referred to as "realising" the list). A view isn't always the right choice for the job. Views are great for traversal but don't support functions that might access the collection in a random order (for example, sorting). Storing unevaluated thunks can also sometimes be more costly then simply evaluating the collection to begin with, for example situations where we might build a lot of views in one go.
