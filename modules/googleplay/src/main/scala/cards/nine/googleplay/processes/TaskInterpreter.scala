package cards.nine.googleplay.processes

import cats.data.Xor
import cats.instances.list._
import cats.syntax.traverse._
import cats.~>
import cards.nine.commons.TaskInstances._
import cards.nine.googleplay.extracats._
import cards.nine.googleplay.domain._
import scalaz.concurrent.Task

class TaskInterpreter(
  itemService: AppRequest ⇒ Task[Xor[String, Item]],
  appCardService: AppRequest ⇒ Task[Xor[InfoError, FullCard]],
  recommendByCategory: GooglePlay.RecommendationsByCategory ⇒ Task[Xor[InfoError, FullCardList]],
  recommendByAppList: GooglePlay.RecommendationsByAppList ⇒ Task[FullCardList]
) extends (GooglePlay.Ops ~> Task) {

  def apply[A](fa: GooglePlay.Ops[A]): Task[A] = fa match {

    case GooglePlay.Resolve(auth, pkg) ⇒
      itemService(AppRequest(pkg, auth)).map(_.toOption)

    case GooglePlay.ResolveMany(auth, PackageList(packageNames)) ⇒
      for /*Task*/ {
        xors ← packageNames.traverse { pkg ⇒
          itemService(AppRequest(Package(pkg), auth))
        }
        (errors, apps) = splitXors[String, Item](xors)
      } yield PackageDetails(errors, apps)

    case GooglePlay.GetCard(auth, pkg) ⇒
      appCardService(AppRequest(pkg, auth))

    case GooglePlay.GetCardList(auth, PackageList(packageNames)) ⇒
      for /*Task*/ {
        xors ← packageNames.traverse { pkg ⇒
          appCardService(AppRequest(Package(pkg), auth))
        }
        (errors, apps) = splitXors[InfoError, FullCard](xors)
      } yield FullCardList(errors.map(_.message), apps)

    case message @ GooglePlay.RecommendationsByCategory(_, _) ⇒
      recommendByCategory(message)

    case message @ GooglePlay.RecommendationsByAppList(_, _) ⇒
      recommendByAppList(message)
  }

}