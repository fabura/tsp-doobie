package com.example

import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.util.transactor.Transactor
import scalaz.zio.Task
import doobie.implicits._
import scalaz.zio._
import scalaz.zio.console._

object Attemp2 extends App {

  import scalaz.zio.interop.catz._
  val program = for {
    container <-  IO.succeed(PostgreSQLContainer())

    _ <- IO.succeed(container.start())

    xa = Transactor.fromDriverManager[Task](
      container.driverClassName,
      container.jdbcUrl,
      container.username,
      container.password,
    )
    databaseInterface = DatabaseInterface(xa)
    _ <- databaseInterface.createTable
    _ <- databaseInterface.create(User(1,"Anton"))
    _ <- databaseInterface.create(User(2,"Sergey"))
    anton <- databaseInterface.getAll
    _    <- putStrLn(anton.toString)
  } yield()

  override def run(args: List[String]): ZIO[Attemp2.Environment, Nothing, Int] = {
    program.fold(_ => 1, _ => 0)
  }
}

case class DatabaseInterface(tnx: Transactor[Task]) {
  import scalaz.zio.interop.catz._

  val createTable: Task[Unit] =
    sql"""CREATE TABLE IF NOT EXISTS Users (id int PRIMARY KEY, name varchar)""".update
      .run.transact(tnx).foldM(err => Task.fail(err), _ => Task.succeed(()))

  def get(id: Int): Task[User] = {
    sql"""SELECT * FROM USERS WHERE ID = $id""".query[User].option.transact(tnx)
      .foldM(Task.fail, maybeUser => Task.require(UserNotFound(id))(Task.succeed(maybeUser)))
  }

  def getAll: Task[List[User]] = {
    sql"""SELECT * FROM USERS""".query[User].to[List].transact(tnx)
      .foldM(err => Task.fail(err), list => Task.succeed(list))
  }
  def getAllStream: Task[List[User]] = {
   fs2.Stream[Task, User] = sql"""SELECT * FROM USERS""".query[User].stream.transact(tnx)
//      .foldM(err => Task.fail(err), list => Task.succeed(list))
  }

  def create(user: User): Task[User] = {
    sql"""INSERT INTO USERS (ID, NAME) VALUES (${user.id}, ${user.name})""".update.run
      .transact(tnx).foldM(err => Task.fail(err), _ => Task.succeed(user))
  }
  object DatabaseInterface {
    def apply(tnx: Transactor[Task]): DatabaseInterface = new DatabaseInterface(tnx)
  }
}

