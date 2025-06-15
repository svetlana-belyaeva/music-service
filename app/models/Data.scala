package models

import models.InMemorySongRepo.songs

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

trait Performer {
  def id: Long
  def name: String
  def cover: Option[String]
  def songs: List[Song]

  def albums: List[Album]
}

case class Singer(
                   id: Long,
                   name: String,
                   cover: Option[String],
                   songs: List[Song],
                   albums: List[Album]
                 ) extends Performer

case class MusicBand(
                      id: Long,
                      name: String,
                      cover: Option[String],
                      songs: List[Song],
                      albums: List[Album],
                      singers: List[Singer], // fixme: proper type?
                    ) extends Performer

case class Song(
                 id: Long,
                 name: String,
                 cover: Option[String],
                 length: Double,
                 file: String,
                 genre: Genre.Value,
                 album: Album
               )

case class Album(
                  id: Long,
                  name: String,
                  cover: Option[String]
                )

class SongRepo {
  def allSongs(): Seq[Song] = songs
  def getSong(nameSubstring: String): Seq[Song] = songs.filter(_.name.toLowerCase.contains(nameSubstring.toLowerCase))
  def getSongByGenre(genre: Genre.Value): Seq[Song] = songs.filter(_.genre == genre)

}

object InMemorySongRepo {
  val album = Album(100L, "Help!", Option.empty)

  val songs: Seq[Song] = List(
    Song(1L, "Yesterday", Option.empty, 2.5, "aaaaaaa", Genre.ROCK, album),
    Song(2L, "The Night Before", Option.empty, 3, "bbbbbb", Genre.ROCK, album),
    Song(3L, "Another Girl", Option.empty, 3.5, "ccccccc", Genre.ROCK, album)
  )
}



