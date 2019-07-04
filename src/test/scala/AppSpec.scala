package test.scala

import java.sql.{DriverManager, Connection}

import com.dimafeng.testcontainers.{PostgreSQLContainer, GenericContainer, ForAllTestContainer, MultipleContainers}

import org.scalatest.FlatSpec

class AppSpec extends FlatSpec with ForAllTestContainer {

  val postgres = PostgreSQLContainer()
  val clickhouse = GenericContainer("yandex/clickhouse-server:latest", exposedPorts = Seq(8123, 9000))


  override val container = MultipleContainers(postgres, clickhouse)

  postgres.start()
  val url = postgres.jdbcUrl
  val username = postgres.username
  val password = postgres.password

  var connection: Connection = null

  try {
    Class.forName(postgres.driverClassName)
    connection = DriverManager.getConnection(url, username, password)

    val statementOne = connection.createStatement()
    statementOne.execute("CREATE TABLE test_table (test_column INT); INSERT INTO test_table (test_column) VALUES(5)")

    val statementTwo = connection.createStatement()

    val resultSet = statementTwo.executeQuery("SELECT * FROM test_table;")
    while (resultSet.next()) {
      println(resultSet.getString("test_column"))
      println("host, user")
    }
    println(resultSet)
  }

  println(postgres.jdbcUrl)

  //
  //  "DockerComposeContainer" should "retrieve non-0 port for any of services" in {
  //    Class.forName(postgres.driverClassName)
  //    val connection = DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password)
  //
  //    val preparedStatement = connection.prepareStatement(postgres.testQueryString)
  //    try {
  //      val resultSet = preparedStatement.executeQuery()
  //      resultSet.next()
  //      assert(1 == resultSet.getInt(1))
  //      resultSet.close()
  //    } finally {
  //      preparedStatement.close()
  //      connection.close()
  //    }


  //  }
}