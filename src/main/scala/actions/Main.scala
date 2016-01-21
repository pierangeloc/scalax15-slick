package actions

import slick.dbio.Effect.{Read, Schema, Write}

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import slick.dbio.DBIOAction
import slick.profile.{FixedSqlAction, SqlAction}

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



  // Schema actions -----------------------------

  val createTableAction: FixedSqlAction[Unit, NoStream, Schema] =
    AlbumTable.schema.create

  val dropTableAction: FixedSqlAction[Unit, NoStream, Schema] =
    AlbumTable.schema.drop



  // Select actions -----------------------------

  val selectAction: DBIOAction[Seq[String], NoStream, Effect.Read] =
    AlbumTable
      .filter(_.artist === "Keyboard Cat")
      .map(_.title)
      .result



  // Update actions -----------------------------

  val updateAction: DBIOAction[Int, NoStream, Effect.Write]=
    AlbumTable
      .filter(_.artist === "Keyboard Cat")
      .map(_.title)
      .update("Even Greater Hits")

  val updateAction2: DBIOAction[Int, NoStream, Effect.Write] =
    AlbumTable
      .filter(_.artist === "Keyboard Cat")
      .map(a => (a.title, a.year))
      .update(("Even Greater Hits", 2010))



  // Delete actions -----------------------------

  val deleteAction: DBIOAction[Int, NoStream, Effect.Write] =
    AlbumTable
      .filter(_.artist === "Justin Bieber")
      .delete


  // Insert actions -----------------------------

  val insertOneAction: DBIOAction[Int, NoStream, Effect.Write] =
    AlbumTable += Album("Pink Floyd", "Dark Side of the Moon", 1978, Rating.Awesome )

  val insertAllAction: DBIOAction[Option[Int], NoStream, Effect.Write]  =
    AlbumTable ++= Seq(
      Album( "Keyboard Cat"  , "Keyboard Cat's Greatest Hits" , 2009 , Rating.Awesome ),
      Album( "Spice Girls"   , "Spice"                        , 1996 , Rating.Good    ),
      Album( "Rick Astley"   , "Whenever You Need Somebody"   , 1987 , Rating.NotBad  ),
      Album( "Manowar"       , "The Triumph of Steel"         , 1992 , Rating.Meh     ),
      Album( "Justin Bieber" , "Believe"                      , 2013 , Rating.Aaargh  ))


  val ex1: FixedSqlAction[Option[Int], NoStream, Write] =
    AlbumTable ++= Seq(
      Album( "Pearl Jam"     , "Ten"             , 1991 , Rating.Awesome ),
      Album( "Dire Straits"  , "Communique"      , 1979 , Rating.Awesome ),
      Album( "Eric Clapton"  , "From the Cradle" , 1994 , Rating.Awesome ),
      Album( "Led Zeppelin"  , "Led Zeppelin"    , 1969 , Rating.Awesome )
  )

  def ex2(year: Int): FixedSqlAction[Int, NoStream, Write] =
    AlbumTable
      .filter(_.year >= year)
      .map(_.rating)
      .update(Rating.Meh)

  def ex3(artist: String): FixedSqlAction[Int, NoStream, Write] =
    AlbumTable
        .filter(_.artist === artist)
        .delete

  // Database -----------------------------------

  val db = Database.forConfig("scalaxdb")



  // Let's go! ----------------------------------

  def exec[T](action: DBIO[T]): T =
    Await.result(db.run(action), 2 seconds)

  def addAlbum(artist: String, title: String, year: Int): DBIOAction[Unit, NoStream, Read with Write] = {
    for {
      existing <- AlbumTable
                      .filter {
                        album => album.artist === artist && album.year < year
                      }.result
      rating = existing.length match {
                case 0 => Rating.Awesome
                case _ => Rating.Meh
               }
      inserted <- AlbumTable += Album(artist, title, year, rating)
    } yield ()

  }


  def main(args: Array[String]): Unit = {
    exec(createTableAction)
    val chainedAction = insertAllAction andThen
                        ex1 andThen
                        ex2(2000) andThen
                        ex3("JustinBieber") andThen
                        addAlbum("Pearl Jam"     , "Vitalogy"        , 1994) andThen
                        addAlbum("Pearl Jam"     , "No Code"         , 1996) andThen
                        addAlbum("Pearl Jam"     , "Vs."             , 1993) andThen
                        AlbumTable.result

    exec(chainedAction)
    exec(AlbumTable.result).foreach(println)
  }

}
