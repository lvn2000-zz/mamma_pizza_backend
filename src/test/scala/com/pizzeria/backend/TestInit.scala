
package com.pizzeria.backend

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration

import com.pizzeria.backend.utils.BackendSettings
import com.pizzeria.common.data.entity.{Group, Item, Property}
import com.pizzeria.common.utils.{CommonSettings, CommonUtils}

import akka.util.Timeout

trait TestInit {

  implicit val timeToWait = Duration(10000, TimeUnit.SECONDS)
  implicit val timeout = Timeout(10000, TimeUnit.SECONDS)

  //initializing init values
  Server.main(Array.empty[String])

  val topic = BackendSettings.base_topic

  val testGroupItem = Group(
    id = CommonUtils().generateUUID(),
    title = "Items group",
    cType = CommonSettings.typeGroupData.itemGroup.toString
  )

  val testGroupProperty = Group(
    id = CommonUtils().generateUUID(),
    title = "Properties group",
    cType = CommonSettings.typeGroupData.propertyGroup.toString
  )

  val testItem = Item(
    id = CommonUtils().generateUUID(),
    title = "Test item",
    group = testGroupItem.id

  )

  val testProperty = Property(
    id = CommonUtils().generateUUID(),
    title = "Test property",
    group = testGroupProperty.id,
    typeValue = CommonSettings.typePropertyData.varcharProp.toString
  )

}