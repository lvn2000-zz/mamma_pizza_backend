package com.pizzeria.backend.actor

import scala.concurrent.ExecutionContext.Implicits.global

import com.pizzeria.backend.data.dao.ItemDao
import akka.pattern._

import akka.actor.{Actor, ActorLogging, Props}
import com.pizzeria.common.api.ItemResponse
import com.pizzeria.common.utils.PrettyPrint
import com.pizzeria.common.api.ItemRequest

class ProcessingActor() extends Actor with ActorLogging {

  def receive = {

    case msg: ItemRequest ⇒
      log.info(s"Received item request message for processing ${PrettyPrint.prettyPrint(msg)}")
      ItemDao().getEntityByIds(msg.keyItems).map(itms ⇒ {
        val itemResp = ItemResponse(itms)
        itemResp
      }) pipeTo sender()

    case _ ⇒ Actor.emptyBehavior
  }

}

object ProcessingActor {
  def props() = Props(classOf[ProcessingActor])
}