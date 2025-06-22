package models

import DBSchema._
import slick.jdbc.H2Profile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DAO(db: Database) {
  def album(nameSubstr: String): Future[Seq[models.AlbumExtended]] = {
    val albumsWithSongsQuery = for {
      album <- albums if album.name like s"%$nameSubstr%"
      song <- songs if song.albumId === album.id
    } yield (album, song)
    db.run(albumsWithSongsQuery.result).map(tuple => {
      val groupedByAlbum = tuple.groupBy(_._1)
      groupedByAlbum.map {
        case (album, albumWithSongs) => AlbumExtended(album, albumWithSongs.map(_._2))
      }
    }.toSeq)
  }

  def allSongs(): Future[Seq[models.Song]] = db.run(songs.result)

  def singer(nameSubstr: String): Future[Seq[models.SingerExtended]] = {
    val filteredSingers = for {
      singer <- singers if singer.name like s"%$nameSubstr%"
    } yield singer

    val singerWithSongsQuery = for {
      ((singer, _), song) <- filteredSingers
        .joinLeft(authorToSongs).on((singer, authorToSong) => singer.id === authorToSong.singerId)
        .joinLeft(songs).on((singerToAuthorToSongs, songs) => singerToAuthorToSongs._2.map(_.songId) === songs.id)
    } yield (singer, song)

    db.run(singerWithSongsQuery.result).map(dataTuple => {
      val groupedBySinger = dataTuple.groupBy(_._1)
      groupedBySinger.map {
        case (singer, singersWithSongs) =>
          val songs = singersWithSongs.flatMap {
            case (_, Some(song)) => Some(song)
            case _ => None
          }
          SingerExtended(singer, songs)
      }
    }.toSeq)
  }

  //  def song(nameSubstring: String): Future[Seq[models.Song]] = db.run(songs.result)

  def createUser(newUser: User): Future[Long] = {
    val userWithIdQuery = (users returning users.map(_.id)) into {
      (user, id) => user.copy(id = id)
    } += newUser
    db.run(userWithIdQuery).map(_.id)
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


}
