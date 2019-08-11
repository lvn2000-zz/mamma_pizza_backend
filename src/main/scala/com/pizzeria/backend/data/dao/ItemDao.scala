package com.pizzeria.backend.data.dao

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.pizzeria.backend.Server.dbSlick
import com.pizzeria.common.data.entity.{Histories, Item, ItemFactories, ItemImpls, Items, LinkItemImpls, Properties}

import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery

class ItemDao() extends DataDaoFactory[Item] {

  lazy val items = TableQuery[Items]
  lazy val itemFactories = TableQuery[ItemFactories]
  lazy val itemImpls = TableQuery[ItemImpls]
  lazy val properties = TableQuery[Properties]
  lazy val histories = TableQuery[Histories]
  lazy val linkItemImpls = TableQuery[LinkItemImpls]

  override def getEntityById(id: String) = {
    val q = items.filter(_.id === id)
    dbSlick.run(q.result).map(_.headOption)
  }

  override def getEntityByIds(ids: Vector[String]): Future[Vector[Item]] = {
    val q = items.filter(_.id inSet ids)
    dbSlick.run(q.result).map(r ⇒ r.toVector)
  }

  override def getEntityByTitle(title: String) = {
    val q = items.filter(_.title === title)
    dbSlick.run(q.result).map(_.headOption)
  }

  override def addEntity(entity: Item) = {
    dbSlick.run(items.insertOrUpdate(entity))
  }

  override def updateEntity(entity: Item) = {
    val q = items.filter(_.id === entity.id).update(entity).transactionally
    dbSlick.run(q)
  }

  override def removeEntity(entity: Item) = {

    val q1 = for {
      item ← items.filter(_.id === entity.id)
      itemFactory ← itemFactories.filter(_.idItem === item.id)
      itemImpl ← itemImpls.filter(_.idItem === item.id)
      history ← histories.filter(_.idItemImpl === itemImpl.id)
      linkItemImpl ← linkItemImpls.filter(_.impl === itemImpl.id)
    } yield linkItemImpl

    val action1 = linkItemImpls.filter(_.id in q1.map(_.id)).delete

    val q2 = for {
      item ← items.filter(_.id === entity.id)
      itemFactory ← itemFactories.filter(_.idItem === item.id)
      itemImpl ← itemImpls.filter(_.idItem === item.id)
      history ← histories.filter(_.idItemImpl === itemImpl.id)
    } yield history

    val action2 = histories.filter(_.id in q2.map(_.id)).delete

    val q3 = for {
      item ← items.filter(_.id === entity.id)
      itemFactory ← itemFactories.filter(_.idItem === item.id)
      itemImpl ← itemImpls.filter(_.idItem === item.id)
    } yield itemImpl

    val action3 = itemImpls.filter(_.id in q3.map(_.id)).delete

    val q4 = for {
      item ← items.filter(_.id === entity.id)
      itemFactory ← itemFactories.filter(_.idItem === item.id)
    } yield itemFactory

    val action4 = itemFactories.filter(_.id in q4.map(_.id)).delete

    val action5 = items.filter(_.id === entity.id).delete

    dbSlick.run(
      (for {
        cnt1 ← action1
        cnt2 ← action2
        cnt3 ← action3
        cnt4 ← action4
        cnt5 ← action5
      } yield (cnt1 + cnt2 + cnt3 + cnt4 + cnt5)).transactionally
    )

  }

}

object ItemDao {
  def apply() = new ItemDao()
}
