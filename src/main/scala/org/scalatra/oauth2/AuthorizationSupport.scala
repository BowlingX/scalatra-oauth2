package org.scalatra
package oauth2

import net.liftweb.common._
import model.Enums._
import java.net.URI
import org.scalatra.{Initializable, ScalatraKernel}


trait AuthorizationSupport extends OAuth2Module with Initializable { self: ScalatraKernel with Logging =>

  abstract override def initialize(config: Config) {
    super.initialize(config)
    val authorizePath = "/" + oauthConfig.authorizePath + "/?"
    get(authorizePath) { oauth2Authorize() }
    post(authorizePath) { oauth2Authorize() }
  }

  protected def oauth2Authorize(): Any = {
    validateParams('response_type, 'client_id, 'client_secret, 'scope, 'redirect_uri)
    val cl = client()
    val scope = (params.get('scope) getOrElse "").split(" ").toList
    val (_, redirectUri) = validateRedirectUri(Full(cl))
    val responseType = params.get('response_type) flatMap { rt =>
      try {
        Option(ResponseType.withName(rt))
      } catch {
        case e: NoSuchElementException => {
          None
        }
      }
    } getOrElse (throw new UnsupportedResponseTypeException(stateParam, redirectUri = Some(redirectUri)))
    if (responseType == ResponseType.Code && cl.authorizationType.value == AuthorizationType.Token)
      throw new UnauthorizedClientException(stateParam, redirectUri = Some(redirectUri))
    if(responseType == ResponseType.Token && cl.authorizationType.value == AuthorizationType.Code)
      throw new UnauthorizedClientException(stateParam, redirectUri = Some(redirectUri))
    if(responseType == ResponseType.CodeAndToken && cl.authorizationType.value != AuthorizationType.CodeAndToken)
      throw new UnauthorizedClientException(stateParam, redirectUri = Some(redirectUri))

    if(!scope.filterNot(cl.scope.value.contains).isEmpty)
      throw new InvalidScopeException(stateParam, redirectUri = Some(redirectUri))

    val authCode = "the_token" //provider.createAuthRequest(cl, scope, redirectUri, responseType, stateParam)
    val query = (Map("authorization" -> authCode) /: stateParam) { (m, state) => m + ("state" -> state) }
    val queryString = query.map { case (k, v) => "%s=%s".format(k.urlEncode, v.urlEncode)} mkString "&"
    val loc = new URI(redirectUri.getScheme, redirectUri.getAuthority, redirectUri.getPath, queryString, null).toASCIIString
    status(302)
    response.setHeader("Location", loc)
    "You are being redirected"
  }
}