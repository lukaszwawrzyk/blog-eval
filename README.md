# Stack-safe recursion with cats.Eval

I was once faced with a challenge of building up a tree of some dependencies. (... explain problem ...).The recursive solution seemed most straightforward and easy to implement, however as it turned out, the tree can be potentially very deep and using recursion in naive way would blow up the stack after around (???) elements.

There is a number of solutions of this problem (which ?). In this blog post we will play with Eval from cats. It allows for pretty straightforward rewrite of original recursive method to stack-safe one, with exactly the same overhead as any similar monad has, for example Future or DBIO. Note, that if you are using them, stack safety is not a concern, they already have this behavior, but Future brings asynchronous actions and DBIO is tied to Slick. Eval is a minimal way to do what we want without any extra overhead.
[where to put this] 

This post will also hopefully be a good example of how to rewrite a relatively complex task into the monad world.
