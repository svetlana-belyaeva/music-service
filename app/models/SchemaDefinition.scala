package models

import sangria.macros.derive._
import sangria.schema.{Field, _}

object SchemaDefinition {
  val genreEnum = deriveEnumType[Genre.Value]()

  val albumType = deriveObjectType[Unit, Album]()

  val songType = deriveObjectType[Unit, Song](
    ReplaceField("genre", Field("genre", genreEnum, resolve = _.value.genre)),
    ReplaceField("album", Field("album", albumType, resolve = _.value.album)),
  )

  val nameSubstringArg = Argument("nameSubstring", StringType, description = "name substring to search for some entity")
  val genreArg = Argument("genre", genreEnum, description = "genre argument to search for")

  val queryType: ObjectType[SongRepo, Unit] =
    ObjectType("Query", fields[SongRepo, Unit](
      Field("all_songs", ListType(songType),
        description = Some("Returns all songs"),
        arguments = Nil,
        resolve = c => c.ctx.allSongs()),
      Field("songs", ListType(songType),
        description = Some("Returns songs which names match passed argument"),
        arguments = nameSubstringArg :: Nil,
        resolve = c => c.ctx.getSong(c arg nameSubstringArg)),
      Field("songs_by_genre", ListType(songType),
        description = Some("Returns songs for matching genre"),
        arguments = genreArg :: Nil,
        resolve = c => c.ctx.getSongByGenre(c arg genreArg))
    ))

  val schema: Schema[SongRepo, Unit] = Schema(queryType)
}
