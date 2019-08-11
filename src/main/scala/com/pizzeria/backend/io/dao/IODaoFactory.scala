package com.pizzeria.backend.io.dao

import scala.concurrent.Future
import com.pizzeria.common.api.PizzaMessage

trait IODaoFactory {

  def receive(): Future[PizzaMessage] = Future.failed(new Exception("Not implemented yet"))

  def send(message: PizzaMessage): Future[Boolean] = Future.failed(new Exception("Not implemented yet"))

}
