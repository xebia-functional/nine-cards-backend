package cards.nine.googleplay

import cats.data.Xor

object extracats {

  def splitXors[L, R](xors: List[Xor[L, R]]): (List[L], List[R]) = {
    def splitXor[L, R](xor: Xor[L, R], ps: (List[L], List[R])): (List[L], List[R]) = xor match {
      case Xor.Left(l) ⇒ (l :: ps._1, ps._2)
      case Xor.Right(r) ⇒ (ps._1, r :: ps._2)
    }
    xors.foldRight[(List[L], List[R])]((Nil, Nil))(splitXor)
  }

}
