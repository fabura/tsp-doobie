import com.dimafeng.testcontainers.PostgreSQLContainer
import com.example.{DatabaseInterface, User}
import org.specs2._
import scalaz.zio.DefaultRuntime
import scalaz.zio._


class HelloWorldSpec extends Specification { def is = s2"""

 This is my first test

 This SQL query string should
   be equal 'User(1,Anton)User(2,Sergei)User(3,Ivan)User(4,John)'  $e1
                                                                 """

  val runtime: DefaultRuntime = new DefaultRuntime {}

  def e1 = "User(1,Anton)User(2,Sergei)User(3,Ivan)User(4,John)" must_== runtime.unsafeRun(program)


  val program: ZIO[Any, Throwable, String] = for {
    container   <- ZIO(PostgreSQLContainer())
    _           <- IO.effectTotal(container.start())
    xa          =  DatabaseInterface.getTransactor(container)
    dbInterface =  DatabaseInterface(xa)
    _           <- dbInterface.createTable
    _           <- dbInterface.create(User(1,"Anton"))
    _           <- dbInterface.create(User(2,"Sergei"))
    _           <- dbInterface.create(User(3,"Ivan"))
    _           <- dbInterface.create(User(4,"John"))

    queue       <- dbInterface.getQueue(100)
    queueData   <- queue.takeAll

  } yield queueData.mkString

}