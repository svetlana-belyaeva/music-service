package models

import slick.ast.BaseTypedType
import slick.jdbc.H2Profile.api._
import slick.jdbc.JdbcType

import scala.language.postfixOps


object DBSchema {
  val schema_name = "music_app"

  class AlbumTable(tag: Tag) extends Table[Album](tag, Some(schema_name), "album") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def cover = column[Option[String]]("cover")
    def * = (id, name, cover).mapTo[Album]
  }
  val albums = TableQuery[AlbumTable]

  implicit val genreColumnType: JdbcType[Genre.Value] with BaseTypedType[Genre.Value] = MappedColumnType.base[Genre.Value, String](
    genre => genre.toString.toLowerCase.capitalize,
    s => Genre.withName(s.toUpperCase))

  class SongTable(tag: Tag) extends Table[Song](tag, Some(schema_name), "song") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def cover = column[Option[String]]("cover")
    def length = column[Double]("length")
    def genre = column[Genre.Value]("genre")
    def file = column[String]("file")

//    def albumId = column[Long]("album_id")
//    def album = foreignKey("album_fk", albumId, albums)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)
    def * = (id, name, cover, length, genre, file/*,  album*/).mapTo[Song]
  }
  val songs = TableQuery[SongTable]

  class SingerTable(tag: Tag) extends Table[Singer](tag, Some(schema_name), "singer") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def cover = column[Option[String]]("cover")

    def * = (id, name, cover).mapTo[Singer]
  }
  val singers = TableQuery[SingerTable]

  class AuthorToSongTable(tag: Tag) extends Table[AuthorToSong](tag, Some(schema_name), "author_to_song") {
    def songId = column[Long]("song_id", O.PrimaryKey)
    def singerId = column[Option[Long]]("singer_id", O.PrimaryKey)
    def musicBandId = column[Option[Long]]("music_band_id", O.PrimaryKey)

    def * = (songId, singerId, musicBandId).mapTo[AuthorToSong]
  }
  val authorToSongs = TableQuery[AuthorToSongTable]

  def createDatabase: DAO = {
    val db = Database.forConfig("postgres")

    new DAO(db)

  }

}

