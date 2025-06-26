package models

import models.GraphQLSchema.Arguments._
import models.GraphQLSchema.EnumTypes._
import models.GraphQLSchema.ObjectTypes._
import sangria.macros.derive._
import sangria.schema._
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.marshalling.FromInput.InputObjectResult
import sangria.util.tag.@@
import sangria.execution.deferred.{DeferredResolver, Fetcher, HasId, Relation, RelationIds}
import sangria.execution.{ExceptionHandler => EHandler, _}


object GraphQLSchema {
  object EnumTypes {
    val GenreEnum = deriveEnumType[Genre.Value]()
    val UserRoleType = deriveEnumType[UserRole.Value]()
  }

  object ObjectTypes {
    lazy val SongType: ObjectType[Unit, Song] = deriveObjectType[Unit, Song](
      ReplaceField("genre", Field("genre", EnumTypes.GenreEnum, resolve = _.value.genre)),
      AddFields(Field("album", AlbumType, resolve = c => albumsFetcher.defer(c.value.albumId)))
    )
    val SingerType = deriveObjectType[Unit, Singer]()
    val PerformerExtendedType = ObjectType("singer", "singer with songs and albums",
      fields[Unit, PerformerWithSongs](
        Field("name", StringType, Some("singer name and surname"), resolve = _.value.performer.name),
        Field("cover", OptionType(StringType), Some("singer cover"), resolve = _.value.performer.cover),
        Field("songs", ListType(SongType), Some("songs written by the singer/band"), resolve = _.value.songs),
        Field("albums", ListType(AlbumType), Some("albums written by the singer/band"), resolve = _.value.albums),
        Field("listeningCount", IntType, Some("count of songs listening"), resolve = _.value.listeningCount)
      )
    )
    val MusicBandType = deriveObjectType[Unit, MusicBand]()

    lazy val AlbumType: ObjectType[Unit, Album] = deriveObjectType[Unit, Album](
      AddFields(
        Field("songs", ListType(SongType), resolve = c => songsByAlbumFetcher.deferRelSeq(songsByAlbumRel, c.value.id))
      )
    )
    val UserType = deriveObjectType[Unit, User](
      ReplaceField("role", Field("role", EnumTypes.UserRoleType, resolve = _.value.role)),
    )
  }

  object Arguments {
    val NameSubstringArg = Argument("nameSubstring", StringType, description = "name substring to search for some entity")
    val GenreArg = Argument("genre", EnumTypes.GenreEnum, description = "genre argument to search for")
    val UserIdArg = Argument("userId", LongType)
    val SongIdArg = Argument("songId", LongType)
    val AlbumIdArg = Argument("albumId", LongType)
    val SingerIdArg = Argument("singerId", LongType)
    val BandIdArg = Argument("bandId", LongType)

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
        InputField("role", UserRoleType),
        InputField("nickname", StringType),
        InputField("email", StringType),
        InputField("password", StringType)
      ))
    val UserInputArg: Argument[User] = Argument[User @@ InputObjectResult]("user", userInputType)
    val EmailArg = Argument("email", StringType)
    val PasswordArg = Argument("password", StringType)
  }

  implicit val albumHasId: HasId[Album, Long] = HasId[Album, Long](_.id)
  implicit val songHasId: HasId[Song, Long] = HasId[Song, Long](_.id)
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
      Field("album", ListType(AlbumType),
        description = Some("Returns albums which name match passed argument"),
        arguments = NameSubstringArg :: Nil,
        resolve = c => c.ctx.dao.album(c arg NameSubstringArg)),
      Field("singer", ListType(PerformerExtendedType),
        description = Some("Returns singers with name matching substring"),
        arguments = NameSubstringArg :: Nil,
        resolve = c => c.ctx.dao.singerExtended(c arg NameSubstringArg)),
      Field("musicBand", ListType(PerformerExtendedType),
        description = Some("Returns music bands with name matching substring"),
        arguments = NameSubstringArg :: Nil,
        resolve = c => c.ctx.dao.musicBandsExtended(c arg NameSubstringArg)),
      Field("song", ListType(SongType),
        description = Some("Returns songs which names match passed argument"),
        arguments = NameSubstringArg :: Nil,
        resolve = c => c.ctx.dao.song(c arg NameSubstringArg)),
      Field("songByGenre", ListType(SongType),
        description = Some("Returns songs for matching genre"),
        arguments = GenreArg :: Nil,
        resolve = c => c.ctx.dao.songByGenre(c arg GenreArg)),
      Field("topSongs", ListType(SongType),
        description = Some("Returns top 5 most listened songs for user"),
        tags = Authorized :: Nil,
        arguments = UserIdArg :: Nil,
        resolve = c => c.ctx.dao.top5Songs(c arg UserIdArg)),
    ))

  val mutationType = ObjectType(
    "Mutation",
    fields[MyContext, Unit](
      Field("likeSong", IntType,
        description = Some("User likes a song"),
        arguments = UserIdArg :: SongIdArg :: Nil,
        tags = Authorized :: Nil,
        resolve = c => c.ctx.dao.likeSong(c arg UserIdArg, c arg SongIdArg)
      ),
      Field("likeAlbum", IntType,
        description = Some("User likes an album"),
        arguments = UserIdArg :: AlbumIdArg :: Nil,
        tags = Authorized :: Nil,
        resolve = c => c.ctx.dao.likeAlbum(c arg UserIdArg, c arg AlbumIdArg)
      ),
      Field("likeSinger", IntType,
        description = Some("User likes a singer"),
        arguments = UserIdArg :: SingerIdArg :: Nil,
        tags = Authorized :: Nil,
        resolve = c => c.ctx.dao.likeSinger(c arg UserIdArg, c arg SingerIdArg)
      ),
      Field("likeBand", IntType,
        description = Some("User likes a band"),
        arguments = UserIdArg :: BandIdArg :: Nil,
        tags = Authorized :: Nil,
        resolve = c => c.ctx.dao.likeBand(c arg UserIdArg, c arg BandIdArg)
      ),
      Field("createUser", LongType,
        description = Some("Register user"),
        arguments = UserInputArg :: Nil,
        resolve = c => c.ctx.dao.createUser(c arg UserInputArg)
      ),
      Field("login",
        UserType,
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
