package com.pizzeria.backend.data.dao

import scala.concurrent.ExecutionContext.Implicits.global

import com.pizzeria.backend.Server.dbSlick
import com.pizzeria.common.data.entity.{Histories, ItemFactories, ItemImpls, LinkItemImpls, Properties, Property}

import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery

class PropertyDao() extends DataDaoFactory[Property] {

  lazy val itemFactories = TableQuery[ItemFactories]
  lazy val itemImpls = TableQuery[ItemImpls]
  lazy val properties = TableQuery[Properties]
  lazy val histories = TableQuery[Histories]
  lazy val linkItemImpls = TableQuery[LinkItemImpls]

  override def getEntityById(id: String) = {
    val q = properties.filter(_.id === id)
    dbSlick.run(q.result).map(_.headOption)
  }

  override def getEntityByTitle(title: String) = {
    val q = properties.filter(_.title === title)
    dbSlick.run(q.result).map(_.headOption)
  }

  override def addEntity(entity: Property) = {
    dbSlick.run(properties.insertOrUpdate(entity))
  }

  override def updateEntity(entity: Property) = {
    val q = properties.filter(_.id === entity.id).update(entity).transactionally
    dbSlick.run(q)
  }

  //TODO need to change this logic with logic that use foreign keys
  override def removeEntity(entity: Property) = {

    val q1 = for {
      prop ← properties.filter(_.id === entity.id)
      itemFactory ← itemFactories.filter(_.idProperty === prop.id)
      history ← histories.filter(_.idItemProperty === prop.id)
      itemImpl ← itemImpls.filter(_.id === history.idItemImpl)
      linkItemImpl ← linkItemImpls.filter(_.impl === itemImpl.id)
    } yield (linkItemImpl)

    val action1 = linkItemImpls.filter(_.id in q1.map(_.id)).delete

    val q2 = for {
      prop ← properties.filter(_.id === entity.id)
      itemFactory ← itemFactories.filter(_.idProperty === prop.id)
      history ← histories.filter(_.idItemProperty === prop.id)
      itemImpl ← itemImpls.filter(_.id === history.idItemImpl)
      linkItemImpl ← linkItemImpls.filter(_.impl === itemImpl.id)
    } yield (history)

    val action2 = histories.filter(_.id in q2.map(_.id)).delete

    val q3 = for {
      prop ← properties.filter(_.id === entity.id)
      itemFactory ← itemFactories.filter(_.idProperty === prop.id)
    } yield (itemFactory)

    val action3 = itemFactories.filter(_.id in q3.map(_.id)).delete

    val q4 = properties.filter(_.id === entity.id)

    val action4 = q4.delete

    dbSlick.run(
      (for {
        cnt1 ← action1
        cnt2 ← action2
        cnt3 ← action3
        cnt4 ← action4
      } yield (cnt1 + cnt2 + cnt3 + cnt4)).transactionally
    )
  }

}

object PropertyDao {
  def apply() = new PropertyDao()
}
