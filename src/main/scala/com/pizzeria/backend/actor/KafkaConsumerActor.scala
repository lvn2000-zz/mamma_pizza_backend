package com.pizzeria.backend.actor

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

import com.pizzeria.backend.Server.mat
import com.pizzeria.common.api.{CheckMessages, PizzaMessage, StartWork, StopWork}
import com.pizzeria.common.utils.{CommonUtils, PrettyPrint}

import akka.Done
import akka.actor.{Actor, ActorLogging, Cancellable, Props}
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.stream.scaladsl.{Keep, Sink}
import akka.pattern._
import akka.util.Timeout

class KafkaConsumerActor(consumerSettings: ConsumerSettings[String, Array[Byte]], topic: String) extends Actor with ActorLogging {

  private implicit val executionContext = context.system.dispatcher
  private implicit val askTimeout: Timeout = 60.seconds
  private var checkMessagesSchedule: Option[Cancellable] = None

  val processing = context.actorSelection("/user/processing-actor")

  override def preStart() = {
    startPulling()
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    log.warning(s"Request consumer restarting due to: ${reason.getMessage}}")
  }

  override def postStop = {
    stopPulling()
    log.info("Request consumer stopped")
  }

  private def processMessages(): Future[Done] = {

    Consumer
      .committableSource(consumerSettings, Subscriptions.topics(topic))
      .mapAsync(10) { msg ⇒
        business(msg.record.key, msg.record.value)
          .map(pzMsg ⇒ processing ? pzMsg)
          .map(_ ⇒ msg.committableOffset)
      }
      .mapAsync(5)(offset ⇒ offset.commitScaladsl())
      .toMat(Sink.ignore)(Keep.both)
      .mapMaterializedValue(DrainingControl.apply)
      .run()
      .drainAndShutdown()

    //    Consumer
    //      .committableSource(consumerSettings, Subscriptions.topics(topic))
    //      .mapAsync(1) { msg ⇒
    //        business(msg.record.key, msg.record.value)
    //          .map(pzMsg ⇒ processing ? pzMsg)
    //          .map(_ ⇒ msg.committableOffset)
    //      }
    //      .batch(
    //        max = 20,
    //        CommittableOffsetBatch.apply
    //      )(_.updated(_))
    //      .mapAsync(3)(_.commitScaladsl())
    //      .toMat(Sink.ignore)(Keep.both)
    //      .mapMaterializedValue(DrainingControl.apply)
    //      .run()
    //      .drainAndShutdown()

  }

  private def business(key: String, value: Array[Byte]): Future[PizzaMessage] = {

    CommonUtils().deserVal(value).map(v ⇒ {
      log.info(s"Receved message ${PrettyPrint.prettyPrint(v)}")
      (processing ? v)
        .flatMap {
          case m: PizzaMessage ⇒ Future.successful(m)
          case _               ⇒ Future.failed(new Exception("Error in processing message"))
        }
    })
      .get

  }

  def receive = {

    case _: CheckMessages ⇒
      log.info(s"Request messages signal received..")
      processMessages() pipeTo sender()

    case _: StartWork ⇒
      startPulling()
      sender() ! Done

    case _: StopWork ⇒
      stopPulling()
      sender() ! Done

    case _ ⇒ Actor.emptyBehavior
  }

  private def startPulling() = {

    checkMessagesSchedule = Option(context.system.scheduler.schedule(
      initialDelay = 100.milliseconds,
      interval = 500.milliseconds,
      receiver = context.self,
      message = CheckMessages()
    ))

  }

  private def stopPulling() = {

    checkMessagesSchedule.foreach(_.cancel())
    checkMessagesSchedule = None
  }

}

object KafkaConsumerActor {
  def props(consumerSettings: ConsumerSettings[String, Array[Byte]], topic: String) = Props(classOf[KafkaConsumerActor], consumerSettings, topic)
}
