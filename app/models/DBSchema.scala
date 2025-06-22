package models

import slick.ast.BaseTypedType
import slick.jdbc.H2Profile.api._
import slick.jdbc.JdbcType
import slick.lifted.ProvenShape

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
    def albumId = column[Long]("album_id")

    def * = (id, name, cover, length, genre, file,  albumId).mapTo[Song]

    def album = foreignKey("album_fk", albumId, albums)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)
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
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def songId = column[Long]("song_id")
    def singerId = column[Option[Long]]("singer_id")
    def musicBandId = column[Option[Long]]("music_band_id")

    def * = (songId, singerId, musicBandId).mapTo[AuthorToSong]

    // fixme: add foreign keys?
  }
  val authorToSongs = TableQuery[AuthorToSongTable]

  implicit val userRoleColumnType: JdbcType[UserRole.Value] with BaseTypedType[UserRole.Value] = MappedColumnType.base[UserRole.Value, String](
    userRole => userRole.toString.toLowerCase.capitalize,
    s => UserRole.withName(s.toUpperCase))

  class UserTable(tag: Tag) extends Table[User](tag, Some(schema_name), "user") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def role = column[UserRole.Value]("role", O.SqlType(s"$schema_name.user_role"))
    def nickname = column[String]("nickname")
    def email = column[String]("email")
    def password = column[String]("password")

    def * = (id, role, nickname, email, password).mapTo[User]
  }
  val users = TableQuery[UserTable]

//  class UserListenSongTable(tag: Tag) extends Table[UserListensToSong](tag, Some(schema_name), "user_listen_song") {
//    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
//    def userId = column[Long]("user_id")
//    def songId = column[Long]("song_id")
//
//    def * = (id, userId, songId).mapTo[UserListensToSong]
//
//    // fixme: add foreign keys?
//  }
//  val userListenSong = TableQuery[UserListenSongTable]

  class LikedSongTable(tag: Tag) extends Table[(Long, Long)](tag, Some(schema_name), "liked_song") {
    def userId = column[Long]("user_id")
    def songId = column[Long]("song_id")

    override def * : ProvenShape[(Long, Long)] = (userId, songId)
  }
  val likedSongs = TableQuery[LikedSongTable]

  class LikedAlbumTable(tag: Tag) extends Table[(Long, Long)](tag, Some(schema_name), "liked_album") {
    def userId = column[Long]("user_id")
    def albumId = column[Long]("album_id")

    override def * : ProvenShape[(Long, Long)] = (userId, albumId)
  }
  val likedAlbums = TableQuery[LikedAlbumTable]

  class LikedSingerTable(tag: Tag) extends Table[(Long, Long)](tag, Some(schema_name), "liked_singer") {
    def userId = column[Long]("user_id")
    def singerId = column[Long]("singer_id")

    override def * : ProvenShape[(Long, Long)] = (userId, singerId)
  }
  val likedSingers = TableQuery[LikedSingerTable]

  class LikedBandTable(tag: Tag) extends Table[(Long, Long)](tag, Some(schema_name), "liked_band") {
    def userId = column[Long]("user_id")
    def bandId = column[Long]("band_id")

    override def * : ProvenShape[(Long, Long)] = (userId, bandId)
  }
  val likedBands = TableQuery[LikedBandTable]

  def createDatabase: DAO = {
    val db = Database.forConfig("postgres")

    new DAO(db)

  }

}

