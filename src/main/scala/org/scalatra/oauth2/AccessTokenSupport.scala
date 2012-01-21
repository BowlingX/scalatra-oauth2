package org.scalatra
package oauth2

import org.scalatra.{ScalatraKernel, Initializable}
import net.liftweb.common._
import model.Enums._

trait AccessTokenSupport extends OAuth2Module with Initializable { self: ScalatraKernel with Logging =>

  abstract override def initialize(config: Config) {
    super.initialize(config)
    val accessTokenPath = "/" + oauthConfig.accessTokenPath + "/?"
    get(accessTokenPath) { invalidMethod() }
    post(accessTokenPath) { oauth2IssueToken() }
  }

  private def invalidMethod() {
    response.setHeader("Allow", "POST")
    halt(405, "Only POST is allowed")
  }

  protected def oauth2IssueToken() {
    validateParams('grant_type, 'code, 'client_id, 'client_secret, 'redirect_uri)
    val cl = client()
    val (_, redirectUri) = validateRedirectUri(Full(cl))
    if (cl.authorizationType == AuthorizationType.Code && GrantCode.fromParam(params.get('grant_type)) != GrantCode.AuthorizationCode) {
      throw new InvalidGrantException(stateParam)
    }
  }
}