import cats.effect.IO
import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.implicits._
import doobie.util.ExecutionContexts
import doobie.{Query0, Transactor, Update0}
import scalaz.zio._
import scalaz.zio.blocking.Blocking
import scalaz.zio.clock.Clock
import scalaz.zio.console.putStrLn
import scalaz.zio.interop.catz.taskConcurrentInstances


object Main extends App{

  val postgres = PostgreSQLContainer()
  postgres.start()


  type AppEnvironment = Clock
  type AppTask[A] = TaskR[AppEnvironment, A]

  override def run(args: List[String]): ZIO[Environment, Nothing, Int] = {

    val program: ZIO[Main.Environment, Throwable, Unit] = for {

      blockingEC <- blocking.blockingExecutor.map(_.asEC).provide(Blocking.Live)
      _ <- putStrLn("Yo")

      transactorR = Persistence.mkTransactor(postgres.driverClassName,
        postgres.jdbcUrl, postgres.username, postgres.password)

      operations = ZIO.runtime[AppEnvironment].flatMap { implicit rts =>
        db.createTable
        db.create(User(1, "Bulat"))
        db.create(User(2, "Timur"))
        db.create(User(3, "Vitaly"))

      }


      program <- transactorR.use { transactor =>
        operations.provideSome[Environment] { _ =>
          new Clock.Live with Persistence.Live {
            override protected def tnx = transactor
          }
        }
      }


    } yield program

    program.fold(_ => 1, _ => 0)

  }
}
final case class User(id: Long, name: String)
final case class UserNotFound(id: Int) extends Exception

trait Persistence extends Serializable {
  val userPersistence: Persistence.Service[Any]
}

object Persistence {
  trait Service[R] {
    val createTable: TaskR[R, Unit]
    def get(id: Int): TaskR[R, User]
    def create(user: User): TaskR[R, User]
    def delete(id: Int): TaskR[R, Unit]
  }

  trait Live extends Persistence {
    protected def tnx: Transactor[Task]

    val userPersistence: Service[Any] = new Service[Any] {
      val createTable: Task[Unit] =
        Task.succeed(SQL.createTable.run.transact(tnx))

      def get(id: Int): Task[User] =
        SQL
          .get(id)
          .option
          .transact(tnx)
          .foldM(
            Task.fail,
            maybeUser => Task.require(UserNotFound(id))(Task.succeed(maybeUser))
          )

      def create(user: User): Task[User] =
        SQL
          .create(user)
          .run
          .transact(tnx)
          .foldM(err => Task.fail(err), _ => Task.succeed(user))

      def delete(id: Int): Task[Unit] =
        SQL
          .delete(id)
          .run
          .transact(tnx)
          .unit
          .orDie
    }
    object SQL {

      def createTable: Update0 = sql"""CREATE TABLE IF NOT EXISTS Users (id int PRIMARY KEY, name varchar)""".update

      def get(id: Int): Query0[User] =
        sql"""SELECT * FROM USERS WHERE ID = $id """.query[User]

      def create(user: User): Update0 =
        sql"""INSERT INTO USERS (ID, NAME) VALUES (${user.id}, ${user.name})""".update

      def delete(id: Int): Update0 =
        sql"""DELETE FROM USERS WHERE ID = $id""".update
    }
  }
  def mkTransactor(
                    className: String,
                    jdbcUrl: String,
                    username: String,
                    pass: String
                  ): Managed[Throwable, Transactor[Task]] = {

    implicit val cs = IO.contextShift(ExecutionContexts.synchronous)
    import scalaz.zio.interop.catz._
    val xa: Transactor[Task] =Transactor.fromDriverManager[Task](
      className,
      jdbcUrl,
      username,
      pass,
      ExecutionContexts.synchronous
    )
    val res: Managed[Nothing, Transactor[Task]] = Managed.succeed(xa)
    res
  }

}
object db extends Persistence.Service[Persistence] {
  val createTable: TaskR[Persistence, Unit]        = ZIO.accessM(_.userPersistence.createTable)
  def get(id: Int): TaskR[Persistence, User]       = ZIO.accessM(_.userPersistence.get(id))
  def create(user: User): TaskR[Persistence, User] = ZIO.accessM(_.userPersistence.create(user))
  def delete(id: Int): TaskR[Persistence, Unit]    = ZIO.accessM(_.userPersistence.delete(id))
}
