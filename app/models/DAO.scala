package models

import DBSchema._
import slick.jdbc.H2Profile.api._

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DAO(db: Database) {
  def album(nameSubstr: String): Future[Seq[models.Album]] = {
    val albumsMatchingName = albums.filter(_.name like(s"%$nameSubstr%"))
    db.run(albumsMatchingName.result)
  }

  def album(ids: Seq[Long]): Future[Seq[models.Album]] = {
    val albumsMatchingName = albums.filter(_.id inSet ids)
    db.run(albumsMatchingName.result)
  }

  def singerExtended(nameSubstr: String): Future[Seq[PerformerWithSongs]] = {
    val filteredSingers = for {
      singer <- singers if singer.name like s"%$nameSubstr%"
    } yield singer

    val action = for {
      matchedSingers <- filteredSingers.result

      singersWithData <- DBIO.sequence(matchedSingers.map { singer =>
        for {
          songIds <- authorToSongs.filter(_.singerId === singer.id).map(_.songId).result
          singerSongs <- songs.filter(_.id.inSet(songIds)).result

          albumIds <- authorToAlbums.filter(_.singerId === singer.id).map(_.albumId).result
          singerAlbums <- albums.filter(_.id.inSet(albumIds)).result

          listeningCount <- userListenSongs.filter(_.songId.inSet(songIds)).length.result
        } yield (singer, singerSongs, singerAlbums, listeningCount)
      })
    } yield singersWithData

    db.run(action.map { results =>
      results.map { case (singer, songs, albums, listeningCount) => PerformerWithSongs(singer, songs, albums, listeningCount) }
    })
  }

  def musicBandsExtended(nameSubstr: String): Future[Seq[PerformerWithSongs]] = {
    val filteredSingers = for {
      band <- musicBands if band.name like s"%$nameSubstr%"
    } yield band

    val action = for {
      matchedBands <- filteredSingers.result

      singersWithData <- DBIO.sequence(matchedBands.map { band =>
        for {
          songIds <- authorToSongs.filter(_.musicBandId === band.id).map(_.songId).result
          singerSongs <- songs.filter(_.id.inSet(songIds)).result

          albumIds <- authorToAlbums.filter(_.musicBandId === band.id).map(_.albumId).result
          singerAlbums <- albums.filter(_.id.inSet(albumIds)).result

          listeningCount <- userListenSongs.filter(_.songId.inSet(songIds)).length.result
        } yield (band, singerSongs, singerAlbums, listeningCount)
      })
    } yield singersWithData

    db.run(action.map { results =>
      results.map { case (singer, songs, albums, listeningCount) => PerformerWithSongs(singer, songs, albums, listeningCount) }
    })
  }

  def song(nameSubstring: String): Future[Seq[Song]] = {
    val songsMatchingName = songs.filter(_.name like(s"%$nameSubstring%"))
    db.run(songsMatchingName.result)
  }

  def song(ids: Seq[Long]): Future[Seq[Song]] = {
    val songsMatchingIds = songs.filter(_.id inSet ids)
    db.run(songsMatchingIds.result)
  }

  def songByGenre(genre: Genre.Value): Future[Seq[Song]] = {
    val songsMatchingName = songs.filter(_.genre === genre)
    db.run(songsMatchingName.result)
  }

  def createUser(newUser: User): Future[Long] = {
    val userWithIdQuery = (users returning users.map(_.id)) into {
      (user, id) => user.copy(id = id)
    } += newUser
    db.run(userWithIdQuery).map(_.id)
  }

  def songsByAlbum(albumIds: Seq[Long]): Future[Seq[Song]] = {
    db.run(songs.filter(_.albumId inSet albumIds).result)
  }

  def likeSong(userId: Long, songId: Long): Future[Int] = {
    val insertUserLikesSong = likedSongs += (userId, songId)
    db.run(insertUserLikesSong)
  }

  def likeAlbum(userId: Long, albumId: Long): Future[Int] = {
    val insertUserLikesAlbum = likedAlbums += (userId, albumId)
    db.run(insertUserLikesAlbum)
  }

  def likeSinger(userId: Long, singerId: Long): Future[Int] = {
    val insertUserLikesSinger = likedSingers += (userId, singerId)
    db.run(insertUserLikesSinger)
  }

  def likeBand(userId: Long, bandId: Long): Future[Int] = {
    val insertUserLikesBand = likedBands += (userId, bandId)
    db.run(insertUserLikesBand)
  }

  def authenticate(email: String, password: String): Future[Option[User]] = db.run {
    users.filter(u => u.email === email && u.password === password).result.headOption
  }

  def top5Songs(userId: Long): Future[Seq[Song]] = {
    val topListenedSongIds = userListenSongs
      .filter(_.userId === userId)
      .filter(_.listenedAt >= LocalDateTime.now().minusYears(1))
      .groupBy(_.songId)
      .map { case (songId, group) =>
        (songId, group.length)
      }
      .sortBy(_._2.desc)
      .take(5)

    val topSongs = topListenedSongIds.flatMap { case (songId, _) =>
      songs.filter(_.id === songId)
    }

    db.run(topSongs.result)
  }
}
