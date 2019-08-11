package com.pizzeria.backend

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

import org.slf4j.LoggerFactory

import com.pizzeria.backend.data.dao.GroupDao
import com.pizzeria.backend.utils.BackendSettings
import com.pizzeria.common.api.PizzaFull
import com.pizzeria.common.data.entity.{Group, Groups, Histories, Item, ItemFactories, ItemFactory, ItemImpls, Items, LinkItemImpls, Properties, Property}
import com.pizzeria.common.utils.CommonUtils

import slick.jdbc.PostgresProfile.api._

object DBWrapper extends App {

  private val logger = LoggerFactory.getLogger(this.getClass)

  lazy val groups = TableQuery[Groups]
  lazy val items = TableQuery[Items]
  lazy val properties = TableQuery[Properties]
  lazy val itemFactories = TableQuery[ItemFactories]
  lazy val histories = TableQuery[Histories]
  lazy val linkValueImpls = TableQuery[LinkItemImpls]
  lazy val itemImpls = TableQuery[ItemImpls]

  //GROUPS
  val uuidGroupItems = CommonUtils().generateUUID()
  val uuidGroupIngredients = CommonUtils().generateUUID()
  val uuidGroupPizzaProperties = CommonUtils().generateUUID()
  val uuidGroupIngredientProperties = CommonUtils().generateUUID()

  //ITEMS
  val uuidItemPizza = CommonUtils().generateUUID()
  val uuidItemIngredient = CommonUtils().generateUUID()

  //PROPERTIES
  val uuidPropNamePizza = CommonUtils().generateUUID()
  val uuidPropIngredientsPizza = CommonUtils().generateUUID()
  val uuidPropTypePizza = CommonUtils().generateUUID()
  val uuidPropCostSizeLPizza = CommonUtils().generateUUID()
  val uuidPropCostSizeMPizza = CommonUtils().generateUUID()
  val uuidPropCostSizeXLPizza = CommonUtils().generateUUID()

  val uuidPropNameIngredient = CommonUtils().generateUUID()

  implicit val db: Database = Database.forConfig(path = "db.postgres", config = BackendSettings.config)

  System.exit(
    {

      if (this.args.isEmpty) {
        logger.error("Need to provide any parameter. For example [deleteData] for deleting of all data, [initData] - for deleting and filling of test data.")
        1
      } else {
        this.args.map(a ⇒ {

          a match {

            case "deleteData" ⇒ {
              {
                Await.result(
                  for {
                    _ ← this.deleteTestData()
                    _ ← {
                      logger.info("Shutting down of Slick connection..")
                      db.shutdown
                    }
                  } yield (), Duration.Inf
                )
                0
              }
            }

            case "initStructure" ⇒ {
              {
                Await.result(for {
                  _ ← this.initStructureData()
                  _ ← db.shutdown
                } yield (), Duration.Inf)
                0
              }
            }

            case aaa ⇒
              logger.error(s"${aaa} - wrong argument!")
              1
          }
        }).head
      }
    }
  )

  def listPizzas(): Future[Vector[PizzaFull]] = {

    val query = for {
      groupPizza ← groups.filter(_.cType === BackendSettings.CONST_PIZZA)
      item ← items.filter(_.group === groupPizza.id)
      groupIngredient ← groups.filter(_.cType === BackendSettings.CONST_INGREDIENT)
      ingredient ← items.filter(_.group === groupIngredient.id)
    } yield (item)

    //stub
    Future(Vector.empty[PizzaFull])
  }

  def initStructureData() = {

    createEmptyStructure().andThen {
      case Success(_) ⇒ insertTestData()
      case _          ⇒ Future.successful(0)
    }

  }

  def deleteTestData(): Future[Int] = {

    logger.info("Beginning of deteting data..")

    Try {
      val gGrpAction = groups.result
      val res = db.run(gGrpAction)

      res.map(grps ⇒
        grps.map(g ⇒ GroupDao().removeEntity(g)))
    } match {
      case Success(v) ⇒
        v.flatMap(s ⇒ Future.sequence(s)).map(_.sum)
      case Failure(e) ⇒
        logger.error(e.getMessage)
        Future.successful(0)
    }

  }

  def createEmptyStructure() = {

    //TODO Here need to add of creating tables in database too

    //GROUPS
    val itemGroupsData = Vector[Group](
      Group(id = uuidGroupItems, title = BackendSettings.CONST_PIZZA, cType = BackendSettings.CONST_PIZZA),
      Group(id = uuidGroupIngredients, title = BackendSettings.CONST_INGREDIENT, cType = BackendSettings.CONST_INGREDIENT),
      Group(id = uuidGroupPizzaProperties, title = BackendSettings.CONST_PROPERTY_PIZZA, cType = BackendSettings.CONST_PROPERTY_PIZZA),
      Group(id = uuidGroupIngredientProperties, title = BackendSettings.CONST_PROPERTY_INGREDIENT, cType = BackendSettings.CONST_PROPERTY_INGREDIENT)
    )

    //ITEMS
    val itemStructure = Vector[Item](
      Item(id = uuidItemPizza, title = BackendSettings.CONST_PIZZA, group = uuidGroupItems),
      Item(id = uuidItemIngredient, title = BackendSettings.CONST_INGREDIENT, group = uuidGroupIngredients)
    )

    //PROPERTIES
    val propertiesStructure = Vector[Property](
      Property(id = uuidPropNamePizza, title = "pizza name", group = uuidGroupPizzaProperties, typeValue = "cvalue"),
      Property(id = uuidPropCostSizeLPizza, title = "cost of SizeL pizza", group = uuidGroupPizzaProperties, typeValue = "nvalue"),
      Property(id = uuidPropCostSizeMPizza, title = "cost of SizeM pizza", group = uuidGroupPizzaProperties, typeValue = "nvalue"),
      Property(id = uuidPropCostSizeXLPizza, title = "cost of SizeXL pizza", group = uuidGroupPizzaProperties, typeValue = "nvalue"),
      Property(id = uuidPropNameIngredient, title = "ingredient name", group = uuidGroupIngredientProperties, typeValue = "nvalue"),
      Property(id = uuidPropIngredientsPizza, title = "pizza ingredients", group = uuidGroupPizzaProperties, typeValue = "impl_value", defaultValue = Some(uuidItemIngredient)),
      Property(id = uuidPropTypePizza, title = "type of pizza", group = uuidGroupPizzaProperties, typeValue = "avalue", defaultValue = Some("vegan,meat,spicy,fish"))
    )

    //ITEM_FACTORIES
    val itemFactoriesStructure = Vector[ItemFactory](
      ItemFactory(
        id = CommonUtils().generateUUID(),
        idItem = uuidItemPizza,
        idProperty = uuidPropNamePizza,
        title = "Pizza name"
      ),
      ItemFactory(
        id = CommonUtils().generateUUID(),
        idItem = uuidItemPizza,
        idProperty = uuidPropIngredientsPizza,
        title = "Pizza ingredients"
      ),
      ItemFactory(
        id = CommonUtils().generateUUID(),
        idItem = uuidItemPizza,
        idProperty = uuidPropTypePizza,
        title = "Pizza type"
      ),
      ItemFactory(
        id = CommonUtils().generateUUID(),
        idItem = uuidItemPizza,
        idProperty = uuidPropCostSizeLPizza,
        title = "Pizza cost SizeL"
      ),
      ItemFactory(
        id = CommonUtils().generateUUID(),
        idItem = uuidItemPizza,
        idProperty = uuidPropCostSizeMPizza,
        title = "Pizza cost SizeM"
      ),
      ItemFactory(
        id = CommonUtils().generateUUID(),
        idItem = uuidItemPizza,
        idProperty = uuidPropCostSizeXLPizza,
        title = "Pizza cost SizeXL"
      ),
      ItemFactory(
        id = CommonUtils().generateUUID(),
        idItem = uuidItemIngredient,
        idProperty = uuidPropNameIngredient,
        title = "Ingredient name"
      )
    )

    val res = for {
      cntGroupIns ← db.run(groups.forceInsertAll(itemGroupsData))
      cntItmIns ← db.run(items.forceInsertAll(itemStructure))
      cntPropIns ← db.run(properties.forceInsertAll(propertiesStructure))
      cntItmFactories ← db.run(itemFactories.forceInsertAll(itemFactoriesStructure))
    } yield (cntGroupIns, cntItmIns, cntPropIns, cntItmFactories)

    res.foreach(r ⇒ logger.info(s"cntGroupIns = ${r._1.getOrElse(0)} " +
      s"cntItmIns = ${r._2.getOrElse(0)} " +
      s"cntPropIns = ${r._3.getOrElse(0)} " +
      s"cntItmFactories = ${r._4.getOrElse(0)}"))

    res
  }

  private def insertTestData(): Future[Int] = {

    //    val res = for {
    //      cntPizzaIns ← db.run(items.forceInsertAll(pizzasData))
    //      cntIngIns ← db.run(items.forceInsertAll(ingrediensData))
    //      cntTypePizzaIns ← db.run(items.forceInsertAll(typePizzasData))
    //      cntPizzaIngredients ← db.run(items.forceInsertAll(pizzaIngredientData))
    //      cntPizzaPrices ← db.run(properties.forceInsertAll(pizzaPricesData))
    //      cntpizzaTypes ← db.run(properties.forceInsertAll(pizzaTypesData))
    //      cntHistories ← db.run(histories.forceInsertAll(historyData))
    //    } yield (cntPizzaIns, cntIngIns, cntTypePizzaIns, cntPizzaIngredients, cntPizzaPrices, cntpizzaTypes, cntHistories)
    //
    //    res.foreach(r ⇒ logger.info(s"cntPizzaIns=${r._1.getOrElse(0)}" +
    //      s"cntIngIns =${r._2.getOrElse(0)} " +
    //      s"cntTypePizzaIns = ${r._3.getOrElse(0)} " +
    //      s"cntPizzaIngredients = ${r._4.getOrElse(0)} " +
    //      s"cntPizzaPrices = ${r._5.getOrElse(0)} " +
    //      s"cntpizzaTypes = ${r._6.getOrElse(0)} " +
    //      s"cntHistories = ${r._7.getOrElse(0)}"))
    //
    //    res

    Future.successful(0)
  }

}

