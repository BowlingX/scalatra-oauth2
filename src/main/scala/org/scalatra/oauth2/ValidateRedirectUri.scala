package org.scalatra
package oauth2

import java.net.{URISyntaxException, URI}

object ValidateRedirectUri {
  def apply(candidate: Option[String]) = {
    val cand = candidate getOrElse (throw InvalidRequestException(None, description = Some("Missing redirect_uri parameter")))
    val redir = try { Option(new URI(cand).normalize) } catch { case e: URISyntaxException => None }
    redir map { u =>
      if(!u.isAbsolute)
        throw InvalidRequestException(None, description = Some("Redirect URL must be absolute"))
      if(u.getScheme.toLowerCase(ENGLISH) != "http" && u.getScheme.toLowerCase(ENGLISH) != "https") {
        throw InvalidRequestException(None, description = Some("Redirect URL must point to HTTP/S location"))
      }
      u
    } getOrElse (throw InvalidRequestException(None, description = Some("Redirect URL looks fishy to me")))
  }

}