package queries

import slick.lifted

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import slick.driver.H2Driver.api._

object Main {

  // Tables -------------------------------------

  case class Album(
    artist : String,
    title  : String,
    year   : Int,
    rating : Rating,
    id     : Long = 0L)

  class AlbumTable(tag: Tag) extends Table[Album](tag, "albums") {
    def artist = column[String]("artist")
    def title  = column[String]("title")
    def year   = column[Int]("year")
    def rating = column[Rating]("rating")
    def id     = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def * = (artist, title, year, rating, id) <> (Album.tupled, Album.unapply)
  }

  lazy val AlbumTable = TableQuery[AlbumTable]



  // Example queries ----------------------------

  val createTableAction =
    AlbumTable.schema.create

  val insertAlbumsAction =
    AlbumTable ++= Seq(
      Album( "Keyboard Cat"  , "Keyboard Cat's Greatest Hits" , 2009 , Rating.Awesome ),
      Album( "Spice Girls"   , "Spice"                        , 1996 , Rating.Good    ),
      Album( "Rick Astley"   , "Whenever You Need Somebody"   , 1987 , Rating.NotBad  ),
      Album( "Manowar"       , "The Triumph of Steel"         , 1992 , Rating.Meh     ),
      Album( "Justin Bieber" , "Believe"                      , 2013 , Rating.Aaargh  ))

  val selectAllQuery =
    AlbumTable

  val selectWhereQuery: Query[AlbumTable, Album, Seq] =
    AlbumTable
      .filter(_.rating === (Rating.Awesome : Rating))

  val selectSortedQuery1 =
    AlbumTable
      .sortBy(_.year.asc)

  val selectSortedQuery2 =
    AlbumTable
      .sortBy(a => (a.year.asc, a.rating.asc))

  val selectPagedQuery =
    AlbumTable
      .drop(2).take(1)

  val selectColumnsQuery1: Query[Rep[String], String, Seq] =
    AlbumTable
      .map(_.title)

  val selectColumnsQuery2: Query[(Rep[String], Rep[String]), (String, String), Seq] =
    AlbumTable
      .map(a => (a.artist, a.title))

  val selectCombinedQuery =
    AlbumTable
      .filter(_.artist === "Keyboard Cat")
      .map(_.title)

  //albums released after 1990 with rating NotBad or higher, with titles in ascending order
  val exercise1: Query[AlbumTable, Album, Seq] =
    AlbumTable
      .filter(_.year >= 1990 )
      //option 1
      //.filter(_.rating inSet List(Rating.NotBad, Rating.Good, Rating.Awesome))
      //option 2: being Rating actually an Int, it supports >=. However, the implicit conversion used by default is turning any type T in an expression >= t where t:T into >= Rep[T], therefore it implicitly converts it into a Rep[Rating.NotBad] instead of Rep[Rating], so we must instruct the compiler to interpret Rating.NotBad as a Rating
      .filter(_.rating >= (Rating.NotBad : Rating))
      .sortBy(_.artist)

  val exercise2: Query[Rep[String], String, Seq] =
    AlbumTable
      .sortBy(_.year.asc)
      .map(_.title)
//N.B. if we swap them we get a compilation error. Reason is the following:
  val q0: Query[AlbumTable, Album, Seq] = AlbumTable
  val q1: Query[AlbumTable, Album, Seq] = q0.filter(_.year === 1990)
  val q2: Query[Rep[String], String, Seq] = q0.map(_.title)



  // Returning single/multiple results ----------

  val selectPagedAction1 =
    selectPagedQuery
      .result

  val selectPagedAction2 =
    selectPagedQuery
      .result
      .headOption

  val selectPagedAction3 =
    selectPagedQuery
      .result
      .head



  // Database -----------------------------------

  val db = Database.forConfig("scalaxdb")



  // Let's go! ----------------------------------

  def exec[T](action: DBIO[T]): T =
    Await.result(db.run(action), 2 seconds)

  def createTestAlbums() = {
    exec(createTableAction)
    exec(insertAlbumsAction)
  }

  def main(args: Array[String]): Unit = {
    createTestAlbums()
//    exec(selectCombinedQuery.result).foreach(println)
//    exec(AlbumTable.filter(_.artist === "Justin Bieber").result).foreach(println)

    exec(exercise1.result).foreach(println)
    println("========")
    exec(exercise2.result).foreach(println)
  }

}
