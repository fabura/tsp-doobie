import DatabaseInterface._
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.specs2._
import zio.{DefaultRuntime, _}


class InsertManySpec extends Specification {
  def is = s2"""

 This is my first test

 This SQL query string should
   be equal 'User(1,Anton)User(2,Sergei)User(3,Ivan)User(4,John)'  $e1
   This queue should contain 1kk elements                          $e2
                                                                 """

  val runtime: DefaultRuntime = new DefaultRuntime {}

  def e1 = "User(1,Anton)User(2,Sergei)User(3,Ivan)User(4,John)" must_== runtime.unsafeRun(program)

  val tableValues: List[List[String]] = List(List("id", "int"), List("name", "varchar"))
  val tableName: String = "Users"
  val tableColumns: List[String] = List("id", "name")
  val millionUsers: List[User] = (1 to 500).toList.map(User(_, "Vasya"))


  val program: ZIO[Any, Throwable, String] = for {
    container   <- ZIO(PostgreSQLContainer())
    _           <- IO.effectTotal(container.start())
    xa          =  getTransactor(container)
    dbInterface =  DatabaseInterface(xa)
    _           <- dbInterface.createTable(tableName, tableValues)
    _           <- dbInterface.create(User(1,"Anton"))
    _           <- dbInterface.create(User(2,"Sergei"))
    _           <- dbInterface.create(User(3,"Ivan"))
    _           <- dbInterface.create(User(4,"John"))

    queue       <- dbInterface.getQueue(100)
    queueData   <- queue.takeAll

  } yield queueData.mkString




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