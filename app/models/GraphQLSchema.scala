package models

import sangria.macros.derive._
import sangria.schema.{Field, _}
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.marshalling.FromInput.InputObjectResult
import sangria.util.tag.@@
import sangria.execution.deferred.{DeferredResolver, Fetcher, HasId, Relation, RelationIds}
import sangria.execution.{ExceptionHandler => EHandler, _}


object GraphQLSchema {
  val genreEnum = deriveEnumType[Genre.Value]()

  lazy val songType: ObjectType[Unit, Song] = deriveObjectType[Unit, Song](
    ReplaceField("genre", Field("genre", genreEnum, resolve = _.value.genre)),
    AddFields(Field("album", albumType, resolve = c => albumsFetcher.defer(c.value.albumId)))
  )
  val singerType = deriveObjectType[Unit, Singer]()
  val performerWithSongsType = ObjectType("singer", "singer with songs and albums",
    fields[Unit, PerformerWithSongs](
      Field("name", StringType, Some("singer name and surname"), resolve = _.value.performer.name),
      Field("cover", OptionType(StringType), Some("singer cover"), resolve = _.value.performer.cover),
      Field("songs", ListType(songType), Some("songs written by the singer"), resolve = _.value.songs))
  )
  val musicBandType = deriveObjectType[Unit, MusicBand]()

  lazy val albumType: ObjectType[Unit, Album] = deriveObjectType[Unit, Album](
    AddFields(
      Field("songs", ListType(songType), resolve = c => songsByAlbumFetcher.deferRelSeq(songsByAlbumRel, c.value.id))
    )
  )
  implicit val albumHasId: HasId[Album, Long] = HasId[Album, Long](_.id)
  implicit val songHasId: HasId[Song, Long] = HasId[Song, Long](_.id)

  val nameSubstringArg = Argument("nameSubstring", StringType, description = "name substring to search for some entity")
  val genreArg = Argument("genre", genreEnum, description = "genre argument to search for")

  val songsByAlbumRel = Relation[Song, Long]("byAlbum", song => Seq(song.albumId))
  val albumsFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Long]) => ctx.dao.album(ids)
  )
  val songsByAlbumFetcher = Fetcher.rel(
    (ctx: MyContext, ids: Seq[Long]) => ctx.dao.song(ids),
    (ctx: MyContext, ids: RelationIds[Song]) => ctx.dao.songsByAlbum(ids(songsByAlbumRel))
  )
  val Resolver = DeferredResolver.fetchers(albumsFetcher, songsByAlbumFetcher)

  val ErrorHandler = EHandler {
    case (_, AuthenticationException(message)) => HandledException(message)
    case (_, AuthorizationException(message)) => HandledException(message)
  }

  val queryType: ObjectType[MyContext, Unit] =
    ObjectType("Query", fields[MyContext, Unit](
      Field("album", ListType(albumType),
        description = Some("Returns albums which name match passed argument"),
        arguments = nameSubstringArg :: Nil,
        resolve = c => c.ctx.dao.album(c arg nameSubstringArg)),
      Field("singer", ListType(performerWithSongsType),
        description = Some("Returns singers with name matching substring"),
        arguments = nameSubstringArg :: Nil,
        resolve = c => c.ctx.dao.singerWithSongs(c arg nameSubstringArg)),
      Field("musicBand", ListType(performerWithSongsType),
        description = Some("Returns music bands with name matching substring"),
        arguments = nameSubstringArg :: Nil,
        resolve = c => c.ctx.dao.musicBandsWithSongs(c arg nameSubstringArg)),
      Field("song", ListType(songType),
        description = Some("Returns songs which names match passed argument"),
        arguments = nameSubstringArg :: Nil,
        resolve = c => c.ctx.dao.song(c arg nameSubstringArg)),
      Field("songByGenre", ListType(songType),
        description = Some("Returns songs for matching genre"),
        arguments = genreArg :: Nil,
        resolve = c => c.ctx.dao.songByGenre(c arg genreArg))
    ))

  val userId = Argument("userId", LongType)
  val songId = Argument("songId", LongType)
  val albumId = Argument("albumId", LongType)
  val singerId = Argument("singerId", LongType)
  val bandId = Argument("bandId", LongType)

  val userRoleType = deriveEnumType[UserRole.Value]()
  val userType = deriveObjectType[Unit, User](
    ReplaceField("role", Field("role", userRoleType, resolve = _.value.role)),
  )
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
  val EmailArg = Argument("email", StringType)
  val PasswordArg = Argument("password", StringType)

  val mutationType = ObjectType(
    "Mutation",
    fields[MyContext, Unit](
      Field("likeSong", IntType,
        description = Some("User likes a song"),
        arguments = userId :: songId :: Nil,
        tags = Authorized :: Nil,
        resolve = c => c.ctx.dao.likeSong(c arg userId, c arg songId)
      ),
      Field("likeAlbum", IntType,
        description = Some("User likes an album"),
        arguments = userId :: albumId :: Nil,
        tags = Authorized :: Nil,
        resolve = c => c.ctx.dao.likeAlbum(c arg userId, c arg albumId)
      ),
      Field("likeSinger", IntType,
        description = Some("User likes a singer"),
        arguments = userId :: singerId :: Nil,
        tags = Authorized :: Nil,
        resolve = c => c.ctx.dao.likeSinger(c arg userId, c arg singerId)
      ),
      Field("likeBand", IntType,
        description = Some("User likes a band"),
        arguments = userId :: bandId :: Nil,
        tags = Authorized :: Nil,
        resolve = c => c.ctx.dao.likeBand(c arg userId, c arg bandId)
      ),
      Field("createUser", LongType,
        description = Some("Register user"),
        arguments = userArg :: Nil,
        resolve = c => c.ctx.dao.createUser(c arg userArg)
      ),
      Field("login",
        userType,
        arguments = EmailArg :: PasswordArg :: Nil,
        resolve = ctx => UpdateCtx(
          ctx.ctx.login(ctx.arg(EmailArg), ctx.arg(PasswordArg))){ user =>
          ctx.ctx.copy(currentUser = Some(user))
        }
      )
    )
  )

  val schema: Schema[MyContext, Unit] = Schema(queryType, Some(mutationType))
}
