package com.pizzeria.backend.utils

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer}

import com.typesafe.config.ConfigFactory

import akka.kafka.{ConsumerSettings, ProducerSettings}

object BackendSettings {

  private val baseConfig = ConfigFactory.load()
  private val confResource = "/main/resources/application.conf"

  val config = ConfigFactory.load(confResource).withFallback(baseConfig)

  val base_topic = config.getString("topics.base_topic")
  val request_topic = config.getString("topics.request_topic")
  val response_topic = config.getString("topics.response_topic")

  val item_topic = config.getString("topics.item_topic")
  val implementation_topic = config.getString("topics.implementation_topic")
  val property_topic = config.getString("topics.property_topic")
  val history_topic = config.getString("topics.history_topic")

  val producerSettings =
    ProducerSettings(config.getConfig("akka.kafka.producer"), new StringSerializer, new ByteArraySerializer)
      .withBootstrapServers("localhost:9092")

  val consumerSettings = ConsumerSettings(config.getConfig("akka.kafka.consumer"), new StringDeserializer, new ByteArrayDeserializer)
    .withBootstrapServers("localhost:9092")
    .withGroupId("group1")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val CONST_PIZZA = "pizza"
  val CONST_INGREDIENT = "ingredient"
  val CONST_PROPERTY_PIZZA = "pizza properties"
  val CONST_PROPERTY_INGREDIENT = "ingredient properties"
  val CONST_INGREDIENT_PIZZA = "ingredient_pizza"
  val CONST_PRICE_PIZZA = "price_pizza"
  val CONST_TYPE_PIZZA = "type_pizza"

}