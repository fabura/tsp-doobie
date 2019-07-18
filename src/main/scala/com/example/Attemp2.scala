package com.example

import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import zio.Task
import doobie.implicits._
import doobie.util.update.Update
import fs2.Stream
import zio._
import zio.console._


object Attemp2 extends App {


  val program: ZIO[Console, Throwable, Unit] = for {
    container   <- ZIO(PostgreSQLContainer())
    _           <- IO.effectTotal(container.start())
    xa          =  DatabaseInterface.getTransactor(container)
    dbInterface =  DatabaseInterface(xa)
    _           <- dbInterface.createTable
//    _           <- dbInterface.create(User(1,"Anton"))
//    _           <- dbInterface.create(User(2,"Sergei"))
//    _           <- dbInterface.create(User(3,"Ivan"))
//    _           <- dbInterface.create(User(4,"John"))
//    _           <- dbInterface.insertMany(List(User(1,"Anton"), User(2,"Sergei"), User(3,"Ivan"), User(4,"John")))

//    _           = Stream.randomSeeded(1488).map(User(_, "Yo")).evalMap(user => dbInterface.create(user)).take(100).compile.drain
//    qq           = Stream.randomSeeded(1488).map(User(_, "Yo"))
//    _= Stream.eval(ZIO.effect(qq.map(user => dbInterface.create(user)))).compile.drain
//    _ <- putStr(qq.mkString)

    _           <- dbInterface.insertMany((1 to 100).toList.map(User(_, "Vasya")))

    queue       <- dbInterface.getQueue(50)
    queueData   <- queue.takeAll
    _           <- putStrLn(queueData.mkString)

//    queueData   <- queue.takeAll
//    _           <- putStrLn(queueData.toString)



  } yield()

  override def run(args: List[String]): ZIO[Attemp2.Environment, Nothing, Int] = {
    program.fold(_ => 1, _ => 0)
  }

}

case class DatabaseInterface(tnx: Transactor[Task]) {
  import zio.interop.catz._
  import cats.implicits._


  val createTable: Task[Unit] =
    sql"""CREATE TABLE IF NOT EXISTS Users (id int PRIMARY KEY, name varchar)""".update
      .run.transact(tnx).foldM(err => Task.fail(err), _ => Task.succeed(()))

  def get(id: Int): Task[User] = {
    sql"""SELECT * FROM USERS WHERE ID = $id""".query[User].option.transact(tnx)
      .foldM(Task.fail, maybeUser => Task.require(UserNotFound(id))(Task.succeed(maybeUser)))
  }

  def getAll(): Task[List[User]] = {
    sql"""SELECT * FROM USERS""".query[User].to[List].transact(tnx)
      .foldM(err => Task.fail(err), list => Task.succeed(list))
  }

  def getAllStream(): Stream[Task, User] = {
    sql"""SELECT * FROM USERS""".query[User].stream.transact(tnx)
  }

  def getQueue(queueCapacity: Int): Task[Queue[User]] = {
    for {
      queue <- Queue.bounded[User](queueCapacity)
      q     <- getAllStream().evalMap(user => queue.offer(user).fork.asInstanceOf[Task[Boolean]]).compile.drain
    } yield queue
  }


  def create(user: User): Task[User] = {
    sql"""INSERT INTO USERS (ID, NAME) VALUES (${user.id}, ${user.name})""".update.run
      .transact(tnx).foldM(err => Task.fail(err), _ => Task.succeed(user))
  }

  def insertMany[T: Write](ps: List[T]):  Task[List[T]] = {
    import cats.implicits._
    val sql = "insert into users (id, name) values (?, ?)"
    Update[T](sql).updateMany(ps).transact(tnx).foldM(err => Task.fail(err), _ => Task.succeed(ps))
  }

}
object DatabaseInterface {
  import zio.interop.catz._

  def apply(tnx: Transactor[Task]): DatabaseInterface = new DatabaseInterface(tnx)

  def getTransactor(container: PostgreSQLContainer): Transactor[Task] = Transactor.fromDriverManager[Task](
    container.driverClassName,
    container.jdbcUrl,
    container.username,
    container.password,
  )
}


case class User(id: Long, name: String)
case class UserNotFound(id: Int) extends Exception



