package com.pizzeria.backend.data.dao

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.pizzeria.backend.Server.dbSlick
import com.pizzeria.common.api.ItemWithProperties
import com.pizzeria.common.data.entity.{Histories, Item, ItemFactories, ItemFactory, ItemImpls, Items, LinkItemImpls, Properties, Property}
import com.pizzeria.common.utils.{CommonUtils, PrettyPrint}

import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery

/**
 * This class realize logic of actions have relation to structure of items. (basically work with ItemFactory table)
 */
class ItemStructureDao() {

  lazy val items = TableQuery[Items]
  lazy val itemFactories = TableQuery[ItemFactories]
  lazy val itemImpls = TableQuery[ItemImpls]
  lazy val properties = TableQuery[Properties]
  lazy val histories = TableQuery[Histories]
  lazy val linkItemImpls = TableQuery[LinkItemImpls]

  def getPropertiesForItem(item: Item): Future[Vector[Property]] = {
    val q = for {
      itemFactory ← itemFactories.filter(_.idItem === item.id)
      property ← properties.filter(_.id === itemFactory.idProperty)
    } yield property

    dbSlick.run(q.result).map(_.toVector)
  }

  def addPropertyForItem(item: Item, property: Property): Future[Int] = {

    val action1 = itemFactories.filter(itmf ⇒ itmf.idItem === item.id && itmf.idProperty === property.id).exists.result
    val action2 = itemFactories.insertOrUpdate(ItemFactory(id = CommonUtils().generateUUID(), idItem = item.id, idProperty = property.id, title = s"Property ${property.title} for item ${item.title}"))

    for {
      idExstRec ← dbSlick.run(action1)
      cnt ← if (!idExstRec)
        dbSlick.run(action2.transactionally)
      else
        Future.failed(new Exception(s"There is already property ${PrettyPrint.prettyPrint(property)} for item ${PrettyPrint.prettyPrint(item)}"))
    } yield cnt

  }

  def removePropertyForItem(item: Item, property: Property): Future[Int] = {

    val q1 = for {
      itemImpl ← itemImpls.filter(_.idItem === item.id)
      linkItemImpl ← linkItemImpls.filter(_.impl === itemImpl.id)
    } yield linkItemImpl

    val action1 = linkItemImpls.filter(_.id in q1.map(_.id)).delete

    val q2 = for {
      item ← items.filter(_.id === item.id)
      itemImpl ← itemImpls.filter(_.idItem === item.id)
      history ← histories.filter(_.idItemImpl === itemImpl.id)
    } yield history

    val action2 = histories.filter(_.id in q2.map(_.id)).delete

    val q3 = itemFactories.filter(_.idItem === item.id)

    val action3 = q3.delete

    dbSlick.run(
      (for {
        cnt1 ← action1
        cnt2 ← action2
        cnt3 ← action3
      } yield (cnt1 + cnt2 + cnt3)).transactionally
    )

  }

  def getItemWithProperties(item: Item): Future[Vector[ItemWithProperties]] = {

    val q = for {
      item ← items.filter(_.id === item.id)
      itemFactory ← itemFactories.filter(_.idItem === item.id)
      property ← properties.filter(_.id === itemFactory.idProperty)
    } yield (item, property)

    dbSlick.run(q.result).map(_.groupBy(_._1).map(i ⇒ ItemWithProperties(item = i._1, properties = i._2.map(_._2).toVector)).toVector)
  }

  def getItemWithProperties(idItem: String): Future[Vector[ItemWithProperties]] = {

    for {
      itemOpt ← ItemDao().getEntityById(idItem)
      itmProp ← itemOpt.fold(Future.successful(Vector.empty[ItemWithProperties]))(itm ⇒ getItemWithProperties(itm))
    } yield itmProp

  }

}

object ItemStructureDao {
  def apply() = new ItemStructureDao()
}
