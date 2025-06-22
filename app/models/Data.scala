package models

object UserRole extends Enumeration {
  val ADMIN, USER = Value
}

case class User(
                 id: Long,
                 role: UserRole.Value,
                 nickname: String,
                 email: String,
                 password: String
               )

object Genre extends Enumeration {
  val BLUES, COUNTRY, ELECTRONIC, HIPHOP, JAZZ, POP, RNB, ROCK, METAL, PUNK = Value // fixme: is it necessary to store them here?
}

// fixme: where to use trait?
trait Performer {
  def id: Long

  def name: String

  def cover: Option[String]
}

case class Singer(
                   id: Long,
                   name: String,
                   cover: Option[String]
                 ) extends Performer

case class SingerExtended(
                           singer: Singer,
                           songs: Seq[Song]
                         )

case class MusicBand(
                      id: Long,
                      name: String,
                      cover: Option[String],
                      singers: Seq[Singer], // fixme: proper type?
                    ) extends Performer

case class Song(
                 id: Long,
                 name: String,
                 cover: Option[String],
                 length: Double,
                 genre: Genre.Value,
                 file: String,
                 albumId: Long
               )

case class Album(
                  id: Long,
                  name: String,
                  cover: Option[String]
                )

case class AuthorToSong(
                         songId: Long,
                         singerId: Option[Long],
                         musicBandId: Option[Long]
                       )



