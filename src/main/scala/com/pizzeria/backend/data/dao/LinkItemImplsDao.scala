package com.pizzeria.backend.data.dao

import scala.concurrent.ExecutionContext.Implicits.global

import com.pizzeria.backend.Server.dbSlick
import com.pizzeria.common.data.entity.{LinkItemImpl, LinkItemImpls}

import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery

class LinkItemImplsDao() extends DataDaoFactory[LinkItemImpl] {

  lazy val linkItemImpls = TableQuery[LinkItemImpls]

  override def getEntityById(id: String) = {
    val q = linkItemImpls.filter(_.id === id)
    dbSlick.run(q.result).map(_.headOption)
  }

  override def addEntity(entity: LinkItemImpl) = {
    dbSlick.run(linkItemImpls.insertOrUpdate(entity).transactionally)
  }

  override def removeEntity(entity: LinkItemImpl) = {
    dbSlick.run(linkItemImpls.filter(_.id === entity.id).delete.transactionally)
  }

}

object LinkItemImplsDao {
  def apply() = new LinkItemImplsDao()
}
