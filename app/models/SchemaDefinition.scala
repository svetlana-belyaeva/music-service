package models

import sangria.macros.derive._
import sangria.schema.{Field, _}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.marshalling.FromInput.InputObjectResult
import sangria.util.tag.@@

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

  val userId = Argument("userId", LongType)
  val songId = Argument("songId", LongType)
  val albumId = Argument("albumId", LongType)
  val singerId = Argument("singerId", LongType)
  val bandId = Argument("bandId", LongType)

  val userRoleType = deriveEnumType[UserRole.Value]()
  implicit val userMarshaller: FromInput[User] = new FromInput[User] {
    val marshaller = CoercedScalaResultMarshaller.default

    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      User(
        id = 0,
        role = ad("role").asInstanceOf[UserRole.Value],
        nickname = ad("nickname").asInstanceOf[String],
        email = ad("email").asInstanceOf[String],
        password = ad("password").asInstanceOf[String]
      )
    }
  }
  val userInputType: InputObjectType[User] =
    InputObjectType[User]("user", List(
      InputField("role", userRoleType),
      InputField("nickname", StringType),
      InputField("email", StringType),
      InputField("password", StringType)
    ))
  val userArg: Argument[User] = Argument[User @@ InputObjectResult]("user", userInputType)

  val mutationType = ObjectType(
    "Mutation",
    fields[MyContext, Unit](
      Field("likeSong", IntType,
        description = Some("User likes a song"),
        arguments = userId :: songId :: Nil,
        resolve = c => c.ctx.dao.likeSong(c arg userId, c arg songId)
      ),
      Field("likeAlbum", IntType,
        description = Some("User likes an album"),
        arguments = userId :: albumId :: Nil,
        resolve = c => c.ctx.dao.likeAlbum(c arg userId, c arg albumId)
      ),
      Field("likeSinger", IntType,
        description = Some("User likes a singer"),
        arguments = userId :: singerId :: Nil,
        resolve = c => c.ctx.dao.likeSinger(c arg userId, c arg singerId)
      ),
      Field("likeBand", IntType,
        description = Some("User likes a band"),
        arguments = userId :: bandId :: Nil,
        resolve = c => c.ctx.dao.likeBand(c arg userId, c arg bandId)
      ),
      Field("createUser", LongType,
        description = Some("Register user"),
        arguments = userArg :: Nil,
        resolve = c => c.ctx.dao.createUser(c arg userArg)
      )
    )
  )

  val schema: Schema[MyContext, Unit] = Schema(queryType, Some(mutationType))
}
