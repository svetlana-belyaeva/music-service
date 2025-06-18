package models

import sangria.macros.derive._
import sangria.schema.{Field, _}

object SchemaDefinition {
  val genreEnum = deriveEnumType[Genre.Value]()

  val songType = deriveObjectType[Unit, Song](
    ReplaceField("genre", Field("genre", genreEnum, resolve = _.value.genre))
    //ReplaceField("album", Field("album", albumType, resolve = _.value.album)),
  )
  val singerType = deriveObjectType[Unit, Singer]()
  val singerExtendedType = ObjectType("singer", "singer with songs and albums",
    fields[Unit, SingerExtended](
      Field("name", StringType, Some("singer name and surname"), resolve = _.value.singer.name),
      Field("cover", OptionType(StringType), Some("singer cover"), resolve = _.value.singer.cover),
      Field("songs", ListType(songType), Some("songs written by the singer"), resolve = _.value.songs)
    )
  )

  val albumType = deriveObjectType[Unit, Album]()
  val albumExtendedType = ObjectType("album", "album with songs and albums",
    fields[Unit, AlbumExtended](
      Field("name", StringType, Some("album name"), resolve = _.value.album.name),
      Field("cover", OptionType(StringType), Some("album cover"), resolve = _.value.album.cover),
      Field("songs", ListType(songType), Some("songs in the album"), resolve = _.value.songs)
    )
  )

  val nameSubstringArg = Argument("nameSubstring", StringType, description = "name substring to search for some entity")
  val genreArg = Argument("genre", genreEnum, description = "genre argument to search for")

  val queryType: ObjectType[MyContext, Unit] =
    ObjectType("Query", fields[MyContext, Unit](
      Field("album", ListType(albumExtendedType),
        description = Some("Returns albums which name match passed argument"),
        arguments = nameSubstringArg :: Nil,
        resolve = c => c.ctx.dao.album(c arg nameSubstringArg)),

      Field("all_songs", ListType(songType),
        description = Some("Returns all songs"),
        arguments = Nil,
        resolve = c => c.ctx.dao.allSongs()),

      Field("singer", ListType(singerExtendedType),
        description = Some("Returns singers with name matching substring"),
        arguments = nameSubstringArg :: Nil,
        resolve = c => c.ctx.dao.singer(c arg nameSubstringArg)),

//      Field("songs", ListType(songType),
//        description = Some("Returns songs which names match passed argument"),
//        arguments = nameSubstringArg :: Nil,
//        resolve = c => c.ctx.getSong(c arg nameSubstringArg)),
//      Field("songs_by_genre", ListType(songType),
//        description = Some("Returns songs for matching genre"),
//        arguments = genreArg :: Nil,
//        resolve = c => c.ctx.getSongByGenre(c arg genreArg))
    ))

  val schema: Schema[MyContext, Unit] = Schema(queryType)
}
