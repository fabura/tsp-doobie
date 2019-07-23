import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.implicits._
import doobie.util.{Read, Write}
import doobie.util.transactor.Transactor
import doobie.util.update.Update
import fs2.Stream
import zio.{Task, _}



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





