package com.pizzeria.backend.data.dao

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait DataDaoFactory[T] {

  def getEntityById(id: String): Future[Option[T]] = Future.failed(new Exception("Not implemented yet"))

  def getEntityByIds(ids: Vector[String]): Future[Vector[T]] = Future.failed(new Exception("Not implemented yet"))

  def getEntityByTitle(title: String): Future[Option[T]] = Future.failed(new Exception("Not implemented yet"))

  def addEntity(entity: T): Future[Int] = Future.failed(new Exception("Not implemented yet"))

  def addEntities(entities: Iterable[T]): Future[Int] = Future.failed(new Exception("Not implemented yet"))

  def updateEntity(entity: T): Future[Int] = Future.failed(new Exception("Not implemented yet"))

  def removeEntity(entity: T): Future[Int] = Future.failed(new Exception("Not implemented yet"))

  def removeEntity(id: String): Future[Int] = for {
    optEnt ← getEntityById(id)
    cntDelete ← if (optEnt.isEmpty) {
      Future.failed(new Exception(s"Entity with id = $id doesn't exists!"))
    } else {
      removeEntity(optEnt.get)
    }
  } yield cntDelete

  def removeEntities(entities: Iterable[T]): Future[Int] = Future.sequence(entities.map(this.removeEntity(_))).map(_.sum)

}