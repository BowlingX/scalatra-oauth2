package org.scalatra
package oauth2
package model



object Enums {

  object AuthorizationType extends Enumeration {
    type AuthorizationType = Value
    val Code = Value("code")
    val Token = Value("token")
    val CodeAndToken = Value("code_and_token")
  }

  object ResponseType extends Enumeration {
    type ResponseType = Value
    val Code = Value("code")
    val Token = Value("token")
    val CodeAndToken = Value("code_and_token")
  }

  object GrantCode extends Enumeration {
    type GrantCode = Value
    val None = Value("none")
    val AuthorizationCode = Value("authorization_code")
    val Password = Value("password")
    val ClientCredentials = Value("client_credentials")

    def fromParam(code: Option[String]) = {
      val c = code getOrElse "none"
      try {
        withName(c)
      } catch {
        case e: NoSuchElementException => GrantCode.None
      }
    }
  }

}