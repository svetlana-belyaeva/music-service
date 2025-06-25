package models

import sangria.ast
import sangria.execution.{Middleware, MiddlewareBeforeField, MiddlewareQueryContext}
import sangria.schema.Context

object AuthMiddleware extends Middleware[MyContext] with MiddlewareBeforeField[MyContext] {
  override type QueryVal = Option[Long]
  override type FieldVal = Unit

  val userIdArgName = "userId"

  override def beforeQuery(context: MiddlewareQueryContext[MyContext, _, _]): Option[Long] = {
    context.queryAst.operations.values.iterator
      .flatMap(_.selections)
      .collect { case field: ast.Field => field }
      .flatMap(_.arguments)
      .collectFirst {
        case arg if arg.name == userIdArgName =>
          arg.value match {
            case ast.IntValue(value, _, _) => Some(value.toLong)
            case ast.StringValue(value, _, _, _, _) => value.toLongOption
            case _ => None
          }
      }.flatten
  }

  override def afterQuery(queryVal: QueryVal, context: MiddlewareQueryContext[MyContext, _, _]) = ()

  override def beforeField(
                            queryVal: QueryVal,
                            mctx: MiddlewareQueryContext[MyContext, _, _],
                            ctx: Context[MyContext, _]) = {

    val requireAuth = ctx.field.tags contains Authorized

    if (requireAuth) {
      ctx.ctx.ensureAuthenticated()
      val userIdFromArgVal: Long = queryVal.get
      val userFromCtx: User = ctx.ctx.currentUser.get

      if (userFromCtx.id != userIdFromArgVal)
        throw AuthorizationException(s"User with id ${userFromCtx.id} is trying to load/modify data for user with id=$userIdFromArgVal")
    }

    continue
  }
}
