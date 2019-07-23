import DatabaseInterface._
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.specs2._
import zio.{DefaultRuntime, _}


class InsertOneSpec extends Specification {
  def is = s2"""

 This is my first test

 This SQL query string should
   be equal 'User(1,Anton)User(2,Sergei)User(3,Ivan)User(4,John)'  $e1
                                                                 """

  val runtime: DefaultRuntime = new DefaultRuntime {}

  def e1 = "User(1,Anton)User(2,Sergei)User(3,Ivan)User(4,John)" === runtime.unsafeRun(program)


  val program: ZIO[Any, Throwable, String] = for {
    container   <- ZIO(PostgreSQLContainer())
    _           <- IO.effectTotal(container.start())
    xa          =  getTransactor(container)
    dbInterface =  DatabaseInterface(xa)
    _           <- dbInterface.createTable[User]()
    _           <- dbInterface.insertOne(User(1,"Anton"))
    _           <- dbInterface.insertOne(User(2,"Sergei"))
    _           <- dbInterface.insertOne(User(3,"Ivan"))
    _           <- dbInterface.insertOne(User(4,"John"))

    queue       <- dbInterface.getQueue[User](100)
    firstEl     <- queue.take
    queueData   <- queue.takeAll

  } yield firstEl.toString+queueData.mkString


}