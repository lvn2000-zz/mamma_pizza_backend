package com.pizzeria.backend

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

import org.slf4j.LoggerFactory

import com.pizzeria.backend.actor.{KafkaConsumerActor, KafkaProducerActor, ProcessingActor}
import com.pizzeria.backend.utils.BackendSettings

import akka.actor.{ActorSystem, PoisonPill, Terminated}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import slick.jdbc.PostgresProfile.api.Database

object Server extends App {

  private val log = LoggerFactory.getLogger(this.getClass)

  log.info("Starting system..")

  implicit val askTimeout: Timeout = 10.seconds
  implicit val system = ActorSystem("root")
  implicit val dispatcher = system.dispatcher
  implicit val mat = ActorMaterializer()

  //  implicit lazy val fixedThreadPoolExecutionContext: ExecutionContext = {
  //    val fixedThreadPool: ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors * 2)
  //    ExecutionContext.fromExecutor(fixedThreadPool)
  //  }

  implicit val dbSlick = Database.forConfig(path = "db.postgres ", config = BackendSettings.config)

  val processing = system.actorOf(ProcessingActor.props(), "processing-actor")

  //on this moment used only base_topic
//  val kafkaConsumerBaseTopic = system.actorOf(KafkaConsumerActor.props(BackendSettings.consumerSettings, BackendSettings.base_topic), "kafka-consumer-base-actor")
//  val kafkaProducerBaseTopic = system.actorOf(KafkaProducerActor.props(BackendSettings.producerSettings, BackendSettings.base_topic), "kafka-producer-base-actor")

  val kafkaConsumerRequestTopic = system.actorOf(KafkaConsumerActor.props(BackendSettings.consumerSettings, BackendSettings.request_topic), "kafka-consumer-request-actor")
  val kafkaProducerResponseTopic = system.actorOf(KafkaProducerActor.props(BackendSettings.producerSettings, BackendSettings.response_topic), "kafka-producer-response-actor")
  
  
  //  val kafkaConsumerItemTopic = system.actorOf(KafkaConsumerActor.props(BackendSettings.consumerSettings, BackendSettings.item_topic), "kafka-consumer-item-actor")
  //  val kafkaConsumerImplementationTopic = system.actorOf(KafkaConsumerActor.props(BackendSettings.consumerSettings, BackendSettings.implementation_topic), "kafka-consumer-implementation-actor")
  //  val kafkaConsumerPropertyTopic = system.actorOf(KafkaConsumerActor.props(BackendSettings.consumerSettings, BackendSettings.property_topic), "kafka-consumer-property-actor")
  //  val kafkaConsumerHistoryTopic = system.actorOf(KafkaConsumerActor.props(BackendSettings.consumerSettings, BackendSettings.history_topic), "kafka-consumer-history-actor")
  //
  //  val kafkaProducerItemTopic = system.actorOf(KafkaProducerActor.props(BackendSettings.producerSettings, BackendSettings.item_topic), "kafka-producer-item-actor")
  //  val kafkaProducerImplementationTopic = system.actorOf(KafkaProducerActor.props(BackendSettings.producerSettings, BackendSettings.implementation_topic), "kafka-producer-implementation-actor")
  //  val kafkaProducerPropertyTopic = system.actorOf(KafkaProducerActor.props(BackendSettings.producerSettings, BackendSettings.property_topic), "kafka-producer-property-actor")
  //  val kafkaProducerHistoryTopic = system.actorOf(KafkaProducerActor.props(BackendSettings.producerSettings, BackendSettings.history_topic), "kafka-producer-history-actor")

  sys.addShutdownHook({
    log.info("Stopping system...")

    kafkaConsumerRequestTopic ! PoisonPill
    //    kafkaConsumerItemTopic ! PoisonPill
    //    kafkaConsumerImplementationTopic ! PoisonPill
    //    kafkaConsumerPropertyTopic ! PoisonPill
    //    kafkaConsumerHistoryTopic ! PoisonPill

    processing ! PoisonPill

    kafkaProducerResponseTopic ! PoisonPill
    //    kafkaProducerItemTopic ! PoisonPill
    //    kafkaProducerImplementationTopic ! PoisonPill
    //    kafkaProducerPropertyTopic ! PoisonPill
    //    kafkaProducerHistoryTopic ! PoisonPill

    val exitCode = Await.result(
      system.terminate().flatMap {
        case _: Terminated ⇒
          freeSlickRes().map { _ ⇒
            log.info("System gracefully stopped.")
            0
          }
        case _ ⇒
          freeSlickRes().map { _ ⇒
            log.warn("System didn't stop during allowed time limit. Terminating.")
            1
          }
      }, 10.seconds
    )

  })

  private def freeSlickRes() = {
    log.info("Free all resources allocated by Slick for this Database")
    //dbSlickSession.close()
    dbSlick.shutdown
  }

}