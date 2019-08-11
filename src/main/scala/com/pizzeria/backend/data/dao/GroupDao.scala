package com.pizzeria.backend.data.dao

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.pizzeria.backend.Server.dbSlick
import com.pizzeria.common.data.entity.{Group, Groups, Histories, ItemFactories, ItemImpls, Items, LinkItemImpls, Properties}

import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery

class GroupDao() extends DataDaoFactory[Group] {

  lazy val groups = TableQuery[Groups]
  lazy val items = TableQuery[Items]
  lazy val itemFactories = TableQuery[ItemFactories]
  lazy val itemImpls = TableQuery[ItemImpls]
  lazy val properties = TableQuery[Properties]
  lazy val histories = TableQuery[Histories]
  lazy val linkItemImpls = TableQuery[LinkItemImpls]

  override def getEntityById(id: String) = {
    val q = groups.filter(_.id === id)

    dbSlick.run(q.result).map(_.headOption)
  }

  override def getEntityByTitle(title: String) = {
    val q = groups.filter(_.title === title)
    dbSlick.run(q.result).map(_.headOption)
  }

  override def addEntity(entity: Group): Future[Int] = {
    dbSlick.run(groups.insertOrUpdate(entity))
  }

  //TODO add removing of item_impl for group
  override def removeEntity(entity: Group): Future[Int] = {

    val q1 = for {
      item ← items.filter(_.group === entity.id)
      itemFactory ← itemFactories.filter(_.idItem === item.id)
      itemImpl ← itemImpls.filter(_.idItem === item.id)
      history ← histories.filter(_.idItemImpl === itemImpl.id)
      linkItemImpl ← linkItemImpls.filter(_.impl === itemImpl.id)
    } yield linkItemImpl

    val action1 = linkItemImpls.filter(_.id in q1.map((_.id))).delete

    val q2 = for {
      item ← items.filter(_.group === entity.id)
      itemFactory ← itemFactories.filter(_.idItem === item.id)
      itemImpl ← itemImpls.filter(_.idItem === item.id)
      history ← histories.filter(_.idItemImpl === itemImpl.id)
    } yield history

    val action2 = histories.filter(_.id in q2.map(_.id)).delete

    val q3 = for {
      item ← items.filter(_.group === entity.id)
      itemFactory ← itemFactories.filter(_.idItem === item.id)
      itemImpl ← itemImpls.filter(_.idItem === item.id)
    } yield itemImpl

    val action3 = itemImpls.filter(_.id in q3.map(_.id)).delete

    val q4 = for {
      item ← items.filter(_.group === entity.id)
      itemFactory ← itemFactories.filter(_.idItem === item.id)
    } yield itemFactory

    val action4 = itemFactories.filter(_.id in q4.map(_.id)).delete

    val action5 = items.filter(_.group === entity.id).delete

    val action6 = properties.filter(_.group === entity.id).delete

    val action7 = groups.filter(_.id === entity.id).delete

    //    db.run(action1.flatMap(cnt1 =>
    //      action2.flatMap(cnt2 =>
    //        action3.flatMap(cnt3 =>
    //          action4.flatMap(cnt4 =>
    //            action5.flatMap(cnt5 =>
    //              action6.flatMap(cnt6 =>
    //                action7.map(cnt7 =>
    //                  cnt1 + cnt2 + cnt3 + cnt4 + cnt5 + cnt6 + cnt7))))))).transactionally)

    dbSlick.run(
      (for {
        cnt1 ← action1
        cnt2 ← action2
        cnt3 ← action3
        cnt4 ← action4
        cnt5 ← action5
        cnt6 ← action6
        cnt7 ← action7
      } yield (cnt1 + cnt2 + cnt3 + cnt4 + cnt5 + cnt6 + cnt7)).transactionally
    )

  }

}

object GroupDao {
  def apply() = new GroupDao()
}