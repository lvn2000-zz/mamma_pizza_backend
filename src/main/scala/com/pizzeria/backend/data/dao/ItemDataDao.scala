package com.pizzeria.backend.data.dao

import java.sql.Timestamp
import java.time.Instant

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

import org.slf4j.LoggerFactory

import com.pizzeria.backend.Server.dbSlick

import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery
import com.pizzeria.common.api.PropValue
import com.pizzeria.common.data.entity.Histories
import com.pizzeria.common.data.entity.LinkItemImpls
import com.pizzeria.common.data.entity.ItemFactories
import com.pizzeria.common.api.ImplItemWithProperties
import com.pizzeria.common.data.entity.ItemImpls
import com.pizzeria.backend.utils.BackendSettings
import com.pizzeria.common.data.entity.Items
import com.pizzeria.common.data.entity.ItemImpl
import com.pizzeria.common.data.entity.Properties
import com.pizzeria.common.data.entity.History
import com.pizzeria.common.data.entity.Item
import com.pizzeria.common.utils.CommonSettings
import com.pizzeria.common.utils.CommonUtils

class ItemDataDao() {

  private val log = LoggerFactory.getLogger(this.getClass)

  lazy val items = TableQuery[Items]
  lazy val itemFactories = TableQuery[ItemFactories]
  lazy val itemImpls = TableQuery[ItemImpls]
  lazy val properties = TableQuery[Properties]
  lazy val histories = TableQuery[Histories]
  lazy val linkItemImpls = TableQuery[LinkItemImpls]

  def getImplementationsOfItem(item: Item): Future[Vector[ImplItemWithProperties]] = {

    val q = for {
      item ← items.filter(_.id === item.id)
      itemFactory ← itemFactories.filter(_.idItem === item.id)
      property ← properties.filter(_.id === itemFactory.idProperty)
      itemImpl ← itemImpls.filter(_.idItem === item.id)
      history ← histories.filter(h ⇒ h.idItemImpl === itemImpl.id && h.idItemProperty === property.id)
      //_ ← linkItemImpls.filter(_.impl === itemImpl.id) //TODO need to think about using related values
    } yield (itemImpl, property, history)

    dbSlick.run(q.result).map(_.groupBy(_._1).map(i ⇒
      ImplItemWithProperties(
        itemImpl = i._1,
        properties = i._2.groupBy(_._2).map(p ⇒ {

          val prop = p._1
          val typeValue = prop.typeValue

          p._2.map(s ⇒ {

            val history = s._3
            getValueFromHistory(history, typeValue).flatMap(vAny ⇒

              getTypeValue(typeValue).map(tv ⇒
                PropValue(
                  property = prop,
                  value = vAny,
                  valueType = tv
                )))
          })
        })
          .flatten.flatten.toVector

      )).toVector)

  }

  def newImplementationOfItem(item: Item, properties: Vector[PropValue]): Future[Int] = {

    insertImplementationOfItem(
      ImplItemWithProperties(
        ItemImpl(
          id = CommonUtils().generateUUID(),
          idItem = item.id,
          title = s"Generated new implementation for item ${item.id} ${item.title}"
        ), properties
      )
    )

  }

  def insertImplementationOfItem(impOflItem: ImplItemWithProperties): Future[Int] = {

    (for {
      itemImplExists ← dbSlick.run(itemImpls.filter(_.id === impOflItem.itemImpl.id).exists.result)
      cnt ← if (!itemImplExists)
        ItemImplsDao().addEntity(impOflItem.itemImpl)
      else
        Future.failed(new Exception(s"There is already record in item inpl with id=${impOflItem.itemImpl.id}"))

      history ← Future.successful(
        impOflItem.properties
          .map(p ⇒ {

            val newHistory = History(
              id = CommonUtils().generateUUID(),
              idItemProperty = p.property.id,
              idItemImpl = impOflItem.itemImpl.id,
              dateChange = new Timestamp(Instant.now.toEpochMilli())
            )

            setValueToHistory(history = newHistory, typeData = p.valueType, value = p.value)
          })
      )

    } yield (history, cnt))
      .flatMap(hsts ⇒ HistoryDao().addEntities(hsts._1.flatten).map(_ + hsts._2))

  }

  def updateImplementationOfItem(impOflItem: ImplItemWithProperties): Future[Int] = {

    (for {
      itemImplExists ← dbSlick.run(itemImpls.filter(_.id === impOflItem.itemImpl.id).exists.result)
      cnt ← if (itemImplExists)
        ItemImplsDao().updateEntity(impOflItem.itemImpl)
      else
        Future.failed(new Exception(s"There is already record in item inpl with id=${impOflItem.itemImpl.id}"))

      historyUpdated ← Future.sequence(
        {
          impOflItem.properties
            .map(prop ⇒ {

              val propValue = prop.value
              val propTypeValue = prop.valueType

              val action = histories.filter(hst ⇒ hst.idItemImpl === impOflItem.itemImpl.id && hst.idItemProperty === prop.property.id)

              dbSlick.run(action.result).flatMap(
                _.headOption
                  .flatMap(hs ⇒
                    setValueToHistory(history = hs.copy(dateChange = CommonUtils().timeToTimestamp(Instant.now)), typeData = propTypeValue, value = propValue)
                      .map(v ⇒ dbSlick.run(action.update(v).transactionally))).getOrElse(Future.successful(0))
              )
            })
        }
      ).map(_.sum)

    } yield (historyUpdated, cnt)).map(v ⇒ v._1 + v._2)

  }

  def removeImplementationOfItem(impOflItem: ImplItemWithProperties): Future[Int] = {
    ItemImplsDao().removeEntity(impOflItem.itemImpl)
  }

  def getValueFromHistory(history: History, cTypeData: String): Option[Any] = {

    Try {
      CommonSettings.typePropertyData.withName(cTypeData) match {
        case CommonSettings.typePropertyData.varcharProp ⇒ history.aValue
        case CommonSettings.typePropertyData.textProp    ⇒ history.cValue
        case CommonSettings.typePropertyData.timeProp ⇒
          history.aValue.map(CommonUtils().convertStringToTimestamp(_))
        case CommonSettings.typePropertyData.numberProp ⇒
          history.aValue.map(BigDecimal(_))
        case CommonSettings.typePropertyData.intProp ⇒
          history.aValue.map(_.toInt)
        case CommonSettings.typePropertyData.blobProp ⇒
          history.bValue
        case _ ⇒ None
      }
    } match {
      case Success(v) ⇒ v
      case Failure(ex) ⇒
        log.error(s"Error in getting value from database - ${ex.getMessage}")
        None
    }

  }

  /**
   * Return new history case class based on existing, but with new value as Option
   */
  private def setValueToHistory(history: History, typeData: CommonSettings.typePropertyData.Value, value: Any): Option[History] = {

    Try {

      val tempVal = scala.collection.mutable.Map.empty[String, Option[Any]]

      typeData match {
        case CommonSettings.typePropertyData.varcharProp ⇒ tempVal += "aValue" -> Some(value)
        case CommonSettings.typePropertyData.textProp    ⇒ tempVal += "cValue" -> Some(value)
        case CommonSettings.typePropertyData.timeProp    ⇒ tempVal += "aValue" -> Some(CommonUtils().convertTimestampTostring(value.asInstanceOf[Timestamp]))
        case CommonSettings.typePropertyData.numberProp ⇒
          tempVal += "aValue" -> Some(value.asInstanceOf[BigDecimal].toString)
        case CommonSettings.typePropertyData.intProp ⇒
          tempVal += "aValue" -> Some(value.asInstanceOf[Int].toString)
        case CommonSettings.typePropertyData.blobProp ⇒
          tempVal += "bValue" -> Some(value.asInstanceOf[Array[Byte]])
        case _ ⇒ None
      }

      Some(
        history.copy(
          history.id,
          history.idItemProperty,
          history.idItemImpl,
          history.title,
          history.parent,
          aValue = tempVal.apply("aValue").map(_.asInstanceOf[String]),
          cValue = tempVal.apply("cValue").map(_.asInstanceOf[String]),
          bValue = tempVal.apply("bValue").map(_.asInstanceOf[Array[Byte]]),
          history.implValue,
          history.dateChange
        )
      )

    } match {
      case Success(v) ⇒ v
      case Failure(ex) ⇒
        log.error(s"Error in getting value from database - ${ex.getMessage}")
        None
    }

  }

  private def getTypeValue(cTypeValue: String): Option[CommonSettings.typePropertyData.Value] = {
    Try {
      CommonSettings.typePropertyData.withName(cTypeValue)
    } match {
      case Success(v) ⇒ Some(v)
      case Failure(ex) ⇒
        log.error(s"Error in getting type of value - ${ex.getMessage}")
        None
    }
  }

}

object ItemDataDao {
  def apply() = new ItemDataDao()
}
