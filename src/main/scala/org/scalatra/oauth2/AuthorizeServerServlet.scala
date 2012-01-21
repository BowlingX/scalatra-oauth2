package org.scalatra
package oauth2


class AuthorizeServerServlet extends ScalatraServlet with Logging with AuthorizationSupport {
  protected val oauthConfig = Config.oauth2

  

}