import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.implicits._
import doobie.util.{Read, Write}
import doobie.util.transactor.Transactor
import doobie.util.update.Update
import fs2.Stream
import zio.console._
import zio.{Task, _}

object Main extends App {
  val tableValues: List[List[String]] = List(List("id", "int"), List("name", "varchar"))
  val tableName: String = "Users"
  val tableColumns: List[String] = List("id", "name")
  val millionUsers: List[User] = (1 to 100).toList.map(User(_, "Vasya"))



  val program: ZIO[Console, Throwable, Unit] = for {
    container   <- ZIO(PostgreSQLContainer())
    _           <- IO.effectTotal(container.start())
    xa          =  DatabaseInterface.getTransactor(container)
    dbInterface =  DatabaseInterface(xa)
    _           <- dbInterface.createTable[User]()
    _           <- dbInterface.insertMany(millionUsers)

    queue       <- dbInterface.getQueue[User](50)
    firstEl     <- queue.take

    queueData   <- queue.takeAll
    _           <- putStrLn(firstEl.toString+queueData.mkString)


  } yield()

  override def run(args: List[String]): ZIO[Main.Environment, Nothing, Int] = {
    program.fold(_ => 1, _ => 0)
  }

}


case class DatabaseInterface(tnx: Transactor[Task]) {
  import zio.interop.catz._


  def createTable[T: HasQueries](): Task[Unit] = {
    implicitly[HasQueries[T]].createTableSql().run.transact(tnx).foldM(err => Task.fail(err), _ => Task.succeed(()))
  }

  def get[T: HasQueries](id: Int): Task[T] = {
    implicitly[HasQueries[T]].getSql(id).option.transact(tnx)
      .foldM(Task.fail, maybeUser => Task.require(UserNotFound(id))(Task.succeed(maybeUser)))
  }

  def getAll[T: HasQueries](): Task[List[T]] = {
    implicitly[HasQueries[T]].getAllSql().to[List].transact(tnx)
      .foldM(err => Task.fail(err), list => Task.succeed(list))
  }

  def getAllStream[T: HasQueries](): Stream[Task, T] = {
    implicitly[HasQueries[T]].getAllSql().stream.transact(tnx)
  }

  def getQueue[T: HasQueries: Read](queueCapacity: Int): Task[Queue[T]] = {
    for {
      queue <- Queue.bounded[T](queueCapacity)
      _     <- getAllStream().evalMap(user => queue.offer(user).asInstanceOf[Task[Boolean]]).compile.drain
    } yield queue
  }


  def insertOne[T: HasQueries](t: T): Task[T] = {
    implicitly[HasQueries[T]].insertOneSql(t).run
      .transact(tnx).foldM(err => Task.fail(err), _ => Task.succeed(t))
  }


  def insertMany[T: HasQueries: Write](values: List[T]):  Task[List[T]] = {
    import cats.implicits._
    val sql: String = implicitly[HasQueries[T]].insertManySql(values)
    Update[T](sql).updateMany(values).transact(tnx).foldM(err => Task.fail(err), _ => Task.succeed(values))
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





