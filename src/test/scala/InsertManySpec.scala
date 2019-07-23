import DatabaseInterface._
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.specs2._
import zio.{DefaultRuntime, _}


class InsertManySpec extends Specification {
  def is = s2"""

 This is my first test

 This SQL query string should
   This queue should contain 1kk elements                          $e2
                                                                 """

  val runtime: DefaultRuntime = new DefaultRuntime {}

  val millionUsers: List[User] = (1 to 1000000).toList.map(User(_, "Vasya"))


  def e2 = runtime.unsafeRun(
    for {
      container   <- ZIO(PostgreSQLContainer())
      _           <- IO.effectTotal(container.start())
      xa          =  getTransactor(container)
      dbInterface =  DatabaseInterface(xa)
      _           <- dbInterface.createTable[User]()
      _           <- dbInterface.insertMany(millionUsers)

      queueData  <- dbInterface.getQueue[User](1000000).flatMap(queue => queue.takeAll)

    } yield queueData.size === 1000000
  )

}