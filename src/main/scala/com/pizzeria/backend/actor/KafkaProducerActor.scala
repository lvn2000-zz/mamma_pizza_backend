package com.pizzeria.backend.actor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import org.apache.kafka.clients.producer.ProducerRecord

import com.pizzeria.backend.Server.mat
import com.pizzeria.common.api.PizzaMessage
import com.pizzeria.common.utils.{CommonUtils, PrettyPrint}

import akka.{Done, NotUsed}
import akka.actor.{Actor, ActorLogging, Props}
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Source
import akka.pattern._

class KafkaProducerActor(producerSettings: ProducerSettings[String, Array[Byte]], topic: String) extends Actor with ActorLogging {

  def receive = {

    case pMsg: PizzaMessage ⇒
      log.info(s"Sending message ${PrettyPrint.prettyPrint(pMsg)} to kafka server")
      sendMessage(Vector(pMsg)) pipeTo sender()

    case _ ⇒ Actor.emptyBehavior
  }

  private def sendMessage(pzMessages: Vector[PizzaMessage]): Future[Done] = {

    val src: Source[ProducerRecord[String, Array[Byte]], NotUsed] = Source[PizzaMessage](pzMessages)
      .map(CommonUtils().serlzVal(_))
      .filter(_.nonEmpty)
      .map(optV ⇒ new ProducerRecord[String, Array[Byte]](topic, optV.get))

    src.runWith(Producer.plainSink(producerSettings))
  }

}

object KafkaProducerActor {
  def props(producerSettings: ProducerSettings[String, Array[Byte]], topic: String) = Props(classOf[KafkaProducerActor], producerSettings, topic)
}