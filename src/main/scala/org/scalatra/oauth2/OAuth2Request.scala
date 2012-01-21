package org.scalatra
package oauth2

import javax.servlet.http.HttpServletRequest


object OAuth2Request {
  private val AUTHORIZATION_KEYS = List("Authorization", "HTTP_AUTHORIZATION", "X-HTTP_AUTHORIZATION", "X_HTTP_AUTHORIZATION")
}
class OAuth2Request(request: HttpServletRequest) {

  import OAuth2Request._
  private def authorizationKey = AUTHORIZATION_KEYS.find(request.getHeader(_) != null)
  private var _credentials: Option[(String, String)] = None

  /**
   * A flag to indicate this request has the OAuth2 Authorization header
   */
  def isOAuth2     = (false /: scheme) { (_, sch) => sch.startsWith("oauth") }
  /**
   * A flag to indicate this request has the Basic Authentication Authorization header
   */
  def isBasicAuth  = (false /: scheme) { (_, sch) => sch.startsWith("basic") }

  /**
   * A flag to indicate whether this request provides authentication information
   */
  def providesAuth = authorizationKey.isDefined

  /**
   * Returns the username for this request
   */
  def username     = credentials map { _._1 } getOrElse null

  /**
   * Returns the password for this request
   */
  def password     = credentials map { _._2 } getOrElse null

  /**
   * The authentication scheme for this request
   */
  def scheme       = parts.headOption.map(sch => sch.toLowerCase(ENGLISH))

  /**
   * The elements contained in the header value
   */
  def parts        = authorizationKey map { request.getHeader(_).split(" ", 2).toList } getOrElse Nil

  /**
   * The user provided parts contained in the header value
   */
  def params       = parts.tail.headOption

  /**
   * The credentials for this request
   */
  def credentials  = {
    if (_credentials.isEmpty )
      _credentials = params map { p =>
        if(isBasicAuth) {
          (null.asInstanceOf[(String, String)] /: new String(Base64.decode(p), Utf8).split(":", 2)) { (t, l) =>
            if(t == null) (l, null) else (t._1, l)
          }
        } else {
          (p, null)
        }
      }
    _credentials
  }

}