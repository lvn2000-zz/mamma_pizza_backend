package com.pizzeria.backend.actor

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import com.pizzeria.backend.ActorSystemTest
import com.pizzeria.backend.Server.dbSlick
import com.pizzeria.backend.TestInit
import com.pizzeria.common.api.{CheckMessages, ItemRequest}
import com.pizzeria.backend.data.dao.{GroupDao, ItemDao, PropertyDao}
import com.pizzeria.backend.utils.BackendSettings
import akka.pattern._
import akka.Done
import akka.testkit.TestKit
import com.pizzeria.common.api.StopWork

class KafkaConsumerActorExec extends FlatSpec with Matchers with BeforeAndAfterAll with TestInit {

  val systemTest = new ActorSystemTest()
  val system = systemTest.system

  //  val producer = system.actorSelection("/user/kafka-producer-response-actor")
  //  val processing = system.actorSelection("/user/processing-actor")
  //  val consumer = system.actorSelection("/user/kafka-consumer-request-actor")

  val processing = system.actorOf(ProcessingActor.props(), "processing-actor")
  //same topic for consumer and producer
  val consumer = system.actorOf(KafkaConsumerActor.props(BackendSettings.consumerSettings, BackendSettings.request_topic), "kafka-consumer-request-actor")
  val producer = system.actorOf(KafkaProducerActor.props(BackendSettings.producerSettings, BackendSettings.request_topic), "kafka-producer-response-actor")

  override def beforeAll() = {
  }

  override def afterAll() = {

    Await.result(dbSlick.shutdown, Duration.Inf)

    TestKit.shutdownActorSystem(system)
    systemTest.shutdown()
  }

  "Creating item structure" should
    "return Done after sending of message to Consumer " in {

      //Create item and property
      val resAddGroupItem = Await.result(GroupDao().addEntity(testGroupItem), atMost = timeToWait)
      val resAddGroupProp = Await.result(GroupDao().addEntity(testGroupProperty), atMost = timeToWait)
      val resAddItem = Await.result(ItemDao().addEntity(testItem), atMost = timeToWait)
      val resAddProp = Await.result(PropertyDao().addEntity(testProperty), atMost = timeToWait)

      val consmrStop = Await.result(consumer ? StopWork(), atMost = timeToWait)

      val itmResp = Await.result(processing ? ItemRequest(keyItems = Vector(testItem.id)), atMost = timeToWait)
      val prodAnswer = Await.result(producer ? itmResp, atMost = timeToWait)

      val respFromProc = Await.result(consumer ? CheckMessages(), atMost = timeToWait)

      //removing item and property
      val resDelProp = Await.result(GroupDao().removeEntity(testGroupProperty), atMost = timeToWait)
      val resDelItem = Await.result(GroupDao().removeEntity(testGroupItem), atMost = timeToWait)

      assert(respFromProc == Done)

    }

}
