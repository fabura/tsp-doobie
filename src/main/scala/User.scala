import doobie.util.query.Query0
import doobie.util.update.Update0
import doobie.implicits._

trait HasQueries[T] {

  def createTableSql(): Update0
  def getSql(id: Int): Query0[T]
  def getAllSql(): Query0[T]
  def insertOneSql(t: T): Update0
  def insertManySql(list: List[T]): String
}


case class User(id: Long, name: String)
case class UserNotFound(id: Int) extends Exception



object User {
  implicit def userQueries: HasQueries[User] = new HasQueries[User] {
    override def createTableSql(): Update0 = sql"""CREATE TABLE IF NOT EXISTS Users (id int PRIMARY KEY, name varchar)""".update

    override def getSql(id: Int): Query0[User] = sql"""SELECT * FROM USERS WHERE ID = $id""".query[User]

    override def getAllSql(): Query0[User] = sql"""SELECT * FROM USERS""".query[User]

    override def insertOneSql(user: User): Update0 = sql"""INSERT INTO USERS (ID, NAME) VALUES (${user.id}, ${user.name})""".update

    override def insertManySql(list: List[User]): String =  "insert into users (id, name) values (?, ?)"

  }

}


//
//trait HasQueries[T] {
//
//  def createTableSql(): String
//  def getSql(id: Int): String
//  def getAllSql(): String
//  def insertOneSql(t: T): String
//  def insertManySql(list: List[T]): String
//}
//
//
//case class User(id: Long, name: String)
//case class UserNotFound(id: Int) extends Exception
//
//
//
//object User {
//  implicit def userQueries: HasQueries[User] = new HasQueries[User] {
//    override def createTableSql(): String = "CREATE TABLE IF NOT EXISTS Users (id int PRIMARY KEY, name varchar)"
//
//    override def getSql(id: Int): String = s"SELECT * FROM USERS WHERE ID = $id"
//
//    override def getAllSql(): String = "SELECT * FROM USERS"
//
//    override def insertOneSql(user: User): String = s"INSERT INTO USERS (ID, NAME) VALUES (${user.id}, ${user.name})"
//
//    override def insertManySql(list: List[User]): String =  "insert into users (id, name) values (?, ?)"
//
//  }
//
//}
