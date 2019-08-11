package com.pizzeria.backend.data.dao

import scala.concurrent.ExecutionContext.Implicits.global

import com.pizzeria.backend.Server.dbSlick
import com.pizzeria.common.data.entity.{Histories, ItemFactories, ItemImpl, ItemImpls, Items, LinkItemImpls, Properties}

import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery

class ItemImplsDao() extends DataDaoFactory[ItemImpl] {

  lazy val items = TableQuery[Items]
  lazy val itemFactories = TableQuery[ItemFactories]
  lazy val itemImpls = TableQuery[ItemImpls]
  lazy val properties = TableQuery[Properties]
  lazy val histories = TableQuery[Histories]
  lazy val linkItemImpls = TableQuery[LinkItemImpls]

  override def getEntityById(id: String) = {
    val q = itemImpls.filter(_.id === id)
    dbSlick.run(q.result).map(_.headOption)
  }

  override def getEntityByTitle(title: String) = {
    val q = itemImpls.filter(_.title === title)
    dbSlick.run(q.result).map(_.headOption)
  }

  override def addEntity(entity: ItemImpl) = {
    dbSlick.run(itemImpls.insertOrUpdate(entity))
  }

  override def updateEntity(entity: ItemImpl) = {
    val q = itemImpls.filter(_.id === entity.id).update(entity).transactionally
    dbSlick.run(q)
  }

  override def removeEntity(entity: ItemImpl) = {

    val q1 = for {
      item ← items.filter(_.id === entity.id)
      itemFactory ← itemFactories.filter(_.idItem === item.id)
      itemImpl ← itemImpls.filter(_.idItem === entity.id)
      history ← histories.filter(_.idItemImpl === itemImpl.id)
      linkItemImpl ← linkItemImpls.filter(_.impl === itemImpl.id)
    } yield linkItemImpl

    val action1 = linkItemImpls.filter(_.id in q1.map(_.id)).delete

    val q2 = for {
      item ← items.filter(_.id === entity.id)
      itemFactory ← itemFactories.filter(_.idItem === item.id)
      itemImpl ← itemImpls.filter(_.idItem === entity.id)
      history ← histories.filter(_.idItemImpl === itemImpl.id)
    } yield history

    val action2 = histories.filter(_.id in q2.map(_.id)).delete

    val q3 = itemImpls.filter(_.id === entity.id)

    val action3 = q3.delete

    dbSlick.run(
      (for {
        cnt1 ← action1
        cnt2 ← action2
        cnt3 ← action3
      } yield (cnt1 + cnt2 + cnt3)).transactionally
    )

  }

}

object ItemImplsDao {
  def apply() = new ItemImplsDao()
}
