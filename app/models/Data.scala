package models

import sangria.execution.FieldTag

import java.time.LocalDateTime

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

case class MusicBand(
                      id: Long,
                      name: String,
                      cover: Option[String]
                    ) extends Performer

case class PerformerWithSongs(
                               performer: Performer,
                               songs: Seq[Song]
                         )


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

case class UserListensToSong(
                              id: Long,
                              userId: Long,
                              songId: Long,
                              listenedAt: LocalDateTime
                            )

case class AuthenticationException(message: String) extends Exception(message)
case class AuthorizationException(message: String) extends Exception(message)
case object Authorized extends FieldTag

