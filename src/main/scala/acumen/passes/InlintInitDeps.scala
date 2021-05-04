package acumen
package passes

import scala.collection.mutable.ListBuffer
import util.ASTUtil.exprSubParts

/* Pass to inline dependencies in the private section.  Used as part
 * of the "normalization" passes, but currently not enabled by default. */

object InlineInitDeps {
  def proc(p: Prog) : Prog = {
    Prog(p.defs.map{c => c.copy(priv = procPriv(c.priv))})
  }

  def procPriv(priv: List[Init]) : List[Init] = {
    val processed = new ListBuffer[Init]
    priv.foreach{init => 
      val res = new util.ASTMap {
        override def mapExpr(e: Expr) = e match {
          case Var(name) => lookup(name)
          case e => super.mapExpr(e)
        }
        def lookup(name: Name) : Expr = {
          processed.find{_.x == name} match {
            case Some(Init(_,ExprRhs(e))) => e
            case _ => Var(name)
          }
        }
      }.mapInit(init)
      processed += res
    }
    processed.toList
  }
}
