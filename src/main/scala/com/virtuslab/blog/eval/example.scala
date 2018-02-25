package com.virtuslab.blog.eval

import cats.Eval
import cats.instances.list._
import cats.syntax.traverse._

trait TreeService {

  type NodeId = Long

  case class Node(
    id:       NodeId,
    label:    String,
    children: List[Node]
  )

  def findLabel(nodeId: NodeId): String

  def findChildrenIds(nodeId: NodeId): List[NodeId]

}

trait TreeExample { this: TreeService =>

  def fetchTreeRecursive(rootId: NodeId): Node = {
    val label = findLabel(rootId)
    val childrenIds = findChildrenIds(rootId)
    val children = childrenIds.map(fetchTreeRecursive)
    Node(rootId, label, children)
  }

  def fetchTreeEval(rootId: NodeId): Eval[Node] = {
    val label = findLabel(rootId)
    val childrenIds = findChildrenIds(rootId)
    // so why is defer needed?
    // childrenIds.traverse(fetchTreeEval) is like
    // childrenIds.map(fetchTreeEval).sequence
    // so childrenIds.map(fetchTreeEval) is called
    // so we are doing recursive call here, like in the naive version
    // we are hoping for the List(Eval(...), Eval(...), Eval(...)) that we get back will contain lazy evals
    // but actually, in order to construct each of those eval, we will calculate the results immedietly
    // there is nothing that would defer the calculations in the code.
    // so let us defer it
    Eval.defer(childrenIds.traverse(fetchTreeEval)).map { children =>
      Node(rootId, label, children)
    }
  }


  // still works cause flatMap is lazy
  def fetchTreeEval2(rootId: NodeId): Eval[Node] = {
    for {
      label       <- Eval.now(findLabel(rootId))
      childrenIds <- Eval.now(findChildrenIds(rootId))
      children    <- childrenIds.traverse(fetchTreeEval2)
    } yield Node(rootId, label, children)
  }

}

// to play more with cats: what if getLabel returns Option because Id can be invalid?

trait GeneratedTree extends TreeService {

  def depth: Int
  def childrenPerNode: Int

  override def findLabel(nodeId: NodeId): String = s"Label($nodeId)"

  val rootNode: NodeId = 0L

  override def findChildrenIds(nodeId: NodeId): List[NodeId] = {
    if (nodeId == depth) Nil else List.fill(childrenPerNode)(nodeId + 1)
  }

}

object TreeTest extends App with TreeExample with GeneratedTree {
  def attempt[A](name: String)(a: => A): A = {
    val res = try Some(a) catch { case _: StackOverflowError => None }

    res match {
      case Some(r) =>
        println(s"$name succeeded!")
        r
      case None =>
        println(s"$name failed ://")
        null.asInstanceOf[A]
    }
  }

  val depth = 1500
  val childrenPerNode = 1

  val safeTree = attempt("eval")(fetchTreeEval(rootNode).value)
  val safeTree2 = attempt("eval2")(fetchTreeEval2(rootNode).value)
  val unsafeTree = attempt("naive")(fetchTreeRecursive(rootNode))

  if (safeTree != unsafeTree) println("trees are different :/")

}