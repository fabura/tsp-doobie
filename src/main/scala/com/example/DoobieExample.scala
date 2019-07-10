package com.example

import cats.effect.IO
import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.util.transactor.Transactor
import doobie._
import doobie.implicits._
import doobie.util.ExecutionContexts

import scala.concurrent.ExecutionContext

object DoobieExample  extends App {

  val container = PostgreSQLContainer()
  container.start()

  implicit val cs = IO.contextShift(ExecutionContext.global)

  val xa = Transactor.fromDriverManager[IO](
    container.driverClassName, container.jdbcUrl, container.username, container.password
  )
  val create =
    sql"""
    CREATE TABLE Users (id int PRIMARY KEY, name varchar)
  """.update.run.transact(xa).unsafeRunSync

  final case class User(id: Long, name: String)

  def insert(user: User) = sql"""INSERT INTO USERS (ID, NAME) VALUES (${user.id}, ${user.name})""".update

  insert(User(1,"Vova")).run.transact(xa).unsafeRunSync
  insert(User(2,"Petya")).run.transact(xa).unsafeRunSync

  sql"select * from USERS"
    .query[User]    // Query0[String]
    .to[List]         // ConnectionIO[List[String]]
    .transact(xa)     // IO[List[String]]
    .unsafeRunSync    // List[String]
    .take(2)          // List[String]
    .foreach(println) // Unit





}
