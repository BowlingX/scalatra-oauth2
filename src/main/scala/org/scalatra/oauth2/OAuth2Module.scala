package mojolly
package oauth2

import org.scalatra.ScalatraKernel
import javax.servlet.http.HttpServletRequest
import model.Client
import net.liftweb.common.{Full, Box}
import java.net.URI

trait OAuth2Module { self: ScalatraKernel with Logging =>
  implicit def request2OAuth2Request(request: HttpServletRequest): OAuth2Request = new OAuth2Request(request)

  protected val oauthConfig: OAuth2Configuration

  def stateParam = params.get('state)

  def validateRedirectUri(cl: Box[Client]) = {
    val uri = ValidateRedirectUri(params.get('redirect_uri) orElse cl.flatMap(_.redirectUri.value.map(new URI(_).toASCIIString)))
    (cl map { c => c.redirectUri.value.forall(u => uri.toASCIIString.startsWith(new URI(u).toASCIIString)) } openOr false, uri)
  }

  def validateParams(names: Symbol*) = {
    names map { name =>
        if (name == 'redirect_uri) {
          val clId = params.get('client_id).getOrElse(throw InvalidRequestException(stateParam, Some("Missing client_id parameter")))
          val (_, uri) = validateRedirectUri(Client.find(clId))
          uri
        }
      params.get(name).getOrElse(throw InvalidRequestException(stateParam, Some("Missing %s parameter" format name.name)))
    }
  }

  def client(authenticate: Boolean = true) = {
    val invalidClient = UnauthorizedClientException(params.get('state))

    val (clientId, clientSecret) = {
      (if (request.isBasicAuth)
        request.credentials
      else
        params.get('client_id).map(id => (id, null))) getOrElse (throw invalidClient)
    }

    Client.find(clientId) map { cl =>
      val isNotAuthenticated = (authenticate && (cl.secret.value != params('client_secret) || cl.revoked_?))
      if (isNotAuthenticated){
        val (validRedirectUri, redirectUri) = validateRedirectUri(Full(cl))
        throw UnauthorizedClientException(stateParam, redirectUri = Option(redirectUri))
      }
      cl
    } openOr (throw invalidClient)
  }

  def unauthorizedOAuth(error: Option[OAuth2Exception] = None) = {
    val challenge = """OAuth realm="%s"""" format oauthConfig.realm
    response.setHeader(
      "WWW-Authenticate",
      error map { e =>
        challenge + ", error=\"%s\", error_description=\"%s\"".format(e.error, e.description)
      } getOrElse challenge)
    halt(401, error.flatMap(_.description) getOrElse "")
  }

  error {
    caughtThrowable match {
      case e: OAuth2Exception => {
        if (e.redirectUri.isDefined) {
          status(302)
          response.setHeader("Location", e.toRedirecUrl())
          "You are being redirected"
        } else {
          contentType = "text/plain"
          // Why deprecate something and not provide a good alternative, editing web.xml isn't a good alternative :)
          response.setStatus(400, e.getMessage)
          e.getMessage
        }
      }
    }
  }
}