package com.pizzeria.backend.data.dao

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.pizzeria.backend.Server.dbSlick
import com.pizzeria.common.data.entity.{Histories, History, ItemFactories, ItemImpls, Items, LinkItemImpls, Properties}

import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery

class HistoryDao() extends DataDaoFactory[History] {

  lazy val items = TableQuery[Items]
  lazy val itemFactories = TableQuery[ItemFactories]
  lazy val itemImpls = TableQuery[ItemImpls]
  lazy val properties = TableQuery[Properties]
  lazy val histories = TableQuery[Histories]
  lazy val linkItemImpls = TableQuery[LinkItemImpls]

  override def getEntityById(id: String) = {
    val q = histories.filter(_.id === id)
    dbSlick.run(q.result).map(_.headOption)
  }

  override def getEntityByTitle(title: String) = {
    val q = histories.filter(h ⇒ h.title.nonEmpty && h.title.get === title)
    dbSlick.run(q.result).map(_.headOption)
  }

  override def addEntity(entity: History) = {
    dbSlick.run(histories.insertOrUpdate(entity))
  }

  override def addEntities(entities: Iterable[History]) = {
    for {
      cnt ← dbSlick.run(histories ++= entities)
      finCnt ← Future.successful(cnt.getOrElse(0))
    } yield finCnt

  }

  override def updateEntity(entity: History) = {
    val q = histories.filter(_.id === entity.id).update(entity).transactionally
    dbSlick.run(q)
  }

  override def removeEntity(entity: History) = {

    val q1 = for {
      history ← histories.filter(_.id === entity.id)
      itemImpl ← itemImpls.filter(_.idItem === history.idItemImpl)
      linkItemImpl ← linkItemImpls.filter(_.impl === itemImpl.id)
    } yield linkItemImpl

    val action1 = linkItemImpls.filter(_.id in q1.map(_.id)).delete

    val action2 = histories.filter(_.id === entity.id).delete

    //db.run(action1.flatMap(cnt1 ⇒ action2.map(cnt2 ⇒ cnt1 + cnt2)).transactionally)
    dbSlick.run(
      (for {
        cnt1 ← action1
        cnt2 ← action2
      } yield (cnt1 + cnt2)).transactionally
    )

  }

  override def removeEntities(entities: Iterable[History]) = {

    val idsDel = entities.map(_.id)
    val q = histories.filter(_.id inSet idsDel)

    dbSlick.run(q.delete)
  }

}

object HistoryDao {
  def apply() = new HistoryDao()
}
