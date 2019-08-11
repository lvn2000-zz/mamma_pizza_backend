package com.pizzeria.backend.data.dao

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import com.pizzeria.backend.ActorSystemTest
import com.pizzeria.backend.Server.{dbSlick ⇒ db}
import com.pizzeria.backend.TestInit
import com.pizzeria.common.api.{ItemWithProperties, PropValue}
import com.pizzeria.common.utils.CommonSettings

import akka.testkit.TestKit

class ItemDataDaoExec extends FlatSpec with Matchers with BeforeAndAfterAll with TestInit {

  val systemTest = new ActorSystemTest()
  val system = systemTest.system

  val groupDao = GroupDao()

  override def beforeAll() = {
  }

  override def afterAll() = {

    Await.result(db.shutdown, Duration.Inf)

    TestKit.shutdownActorSystem(system)
    systemTest.shutdown()
  }

  "Creating item structure" should
    "return item with properties " in {

      //Create item and property
      val resAddGroupItem = Await.result(groupDao.addEntity(testGroupItem), Duration.Inf)
      val resAddGroupProp = Await.result(GroupDao().addEntity(testGroupProperty), Duration.Inf)
      val resAddItem = Await.result(ItemDao().addEntity(testItem), Duration.Inf)
      val resAddProp = Await.result(PropertyDao().addEntity(testProperty), Duration.Inf)

      //removing, creating, readin and finally removing of structure
      val beginResDelStruc = Await.result(ItemStructureDao().removePropertyForItem(testItem, testProperty), Duration.Inf)
      val cntProp = Await.result(ItemStructureDao().addPropertyForItem(testItem, testProperty), Duration.Inf)
      val itmWithProp = Await.result(ItemStructureDao().getItemWithProperties(testItem), Duration.Inf)
      val finishResDelStruc = Await.result(ItemStructureDao().removePropertyForItem(testItem, testProperty), Duration.Inf)

      //removing item and property
      val resDelProp = Await.result(GroupDao().removeEntity(testGroupProperty), Duration.Inf)
      val resDelItem = Await.result(GroupDao().removeEntity(testGroupItem), Duration.Inf)

      assert(itmWithProp.isInstanceOf[Vector[ItemWithProperties]])

    }

  "Creating item structure and data(item implementation" should
    "return history item " ignore {

      //Create item and property
      val resAddGroupItem = Await.result(GroupDao().addEntity(testGroupItem), Duration.Inf)
      val resAddGroupProp = Await.result(GroupDao().addEntity(testGroupProperty), Duration.Inf)
      val resAddItem = Await.result(ItemDao().addEntity(testItem), Duration.Inf)
      val resAddProp = Await.result(PropertyDao().addEntity(testProperty), Duration.Inf)

      //removing, creating, readin and finally removing of structure
      val beginResDelStruc = Await.result(ItemStructureDao().removePropertyForItem(testItem, testProperty), Duration.Inf)
      val cntProp = Await.result(ItemStructureDao().addPropertyForItem(testItem, testProperty), Duration.Inf)
      val vectrItmWithProp = Await.result(ItemStructureDao().getItemWithProperties(testItem), Duration.Inf)

      val itmWithProp = vectrItmWithProp.head

      //      val newImpol = ImplItemWithProperties(itemImpl = itmWithProp.item  ,  properties: itmWithProp.head.head.  Map[Property, Option[PropValue]])

      val itmImpl = Await.result(ItemDataDao().newImplementationOfItem(
        item = testItem,
        properties = Vector(
          PropValue(
            property = testProperty,
            value = "test value",
            valueType = CommonSettings.typePropertyData.varcharProp
          )
        )
      ), Duration.Inf)

      //removing item and property
      val finishResDelStruc = Await.result(ItemStructureDao().removePropertyForItem(testItem, testProperty), Duration.Inf)

      val resDelProp = Await.result(GroupDao().removeEntity(testGroupProperty), Duration.Inf)
      val resDelItem = Await.result(GroupDao().removeEntity(testGroupItem), Duration.Inf)

      assert(itmWithProp.isInstanceOf[Vector[ItemWithProperties]])

    }

}