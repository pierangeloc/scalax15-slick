package tables

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import slick.driver.H2Driver.api._

object Main {

  // Tables -------------------------------------

  class Album (
    val artist: String,
    val title: String,
    val year: Int,
    val id: Long = 0L) {
  }

  def extractAlbum(a: Album) = Some((a.artist, a.title, a.year, a.id))
  def createAlbum(fields: (String, String, Int, Long)) = fields match {
    case (artist, title, year, id) => new Album(artist, title, year, id)
  }

  class AlbumTable(tag: Tag) extends Table[Album](tag, "albums") {
    def artist = column[String]("artist")
    def title  = column[String]("title")
    def year   = column[Int]("year")
    def id     = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def * = (artist, title, year, id) <> (createAlbum, extractAlbum)
  }

  lazy val AlbumTable = TableQuery[AlbumTable]



  // Actions ------------------------------------

  val createTableAction =
    AlbumTable.schema.create

  val insertAlbumsAction =
    AlbumTable ++= Seq(
      new Album( "Keyboard Cat"  , "Keyboard Cat's Greatest Hits"  , 2009), // released in 2009
      new Album( "Spice Girls"   , "Spice"                         , 1996), // released in 1996
      new Album( "Rick Astley"   , "Whenever You Need Somebody"    , 1987), // released in 1987
      new Album( "Manowar"       , "The Triumph of Steel"          , 1992), // released in 1992
      new Album( "Justin Bieber" , "Believe"                       , 2013)) // released in 2013

  val selectAlbumsAction =
    AlbumTable.result



  // Database -----------------------------------
  // use the configuration defined in application.conf relative to scalaxdb
  val db = Database.forConfig("scalaxdb")


  // Let's go! ----------------------------------

  def exec[T](action: DBIO[T]): T =
    Await.result(db.run(action), 2 seconds)

  def main(args: Array[String]): Unit = {
    exec(createTableAction)
    exec(insertAlbumsAction)
    exec(selectAlbumsAction).foreach(println)
  }

}
