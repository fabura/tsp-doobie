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

  val tableValues: List[List[String]] = List(List("id", "int"), List("name", "varchar"))
  val tableName: String = "Users"
  val tableColumns: List[String] = List("id", "name")
  val millionUsers: List[User] = (1 to 500).toList.map(User(_, "Vasya"))


  def e2 = runtime.unsafeRun(
    for {
      container   <- ZIO(PostgreSQLContainer())
      _           <- IO.effectTotal(container.start())
      xa          =  getTransactor(container)
      dbInterface =  DatabaseInterface(xa)
      _           <- dbInterface.createTable(tableName, tableValues)
      _           <- dbInterface.insertMany(tableName, tableColumns, millionUsers)

      queue       <- dbInterface.getQueue(1000000)
      queueData   <- queue.takeAll
    } yield queueData.size must_== 1000000
  )

}