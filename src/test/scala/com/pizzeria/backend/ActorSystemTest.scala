package com.pizzeria.backend

import scala.util.Random

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestKit }

class ActorSystemTest(name: String) extends TestKit(ActorSystem(name)) with TestInit with ImplicitSender {

  def this() = this(s"TestSystem${Random.nextInt(5)}")

  def shutdown(): Unit =
    system.terminate()

}