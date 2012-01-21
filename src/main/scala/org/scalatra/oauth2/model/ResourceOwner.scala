package mojolly
package oauth2
package model

import mongo.fields._
import net.liftweb.mongodb.record.MongoId
import SharedFields.Timestamps
import mongo.{MongoMetaModel, MongoModel}
import net.liftweb.record.field.{IntField, StringField}
import net.liftweb.common._
import net.liftweb.json.JsonDSL._
import uri._
import xml.Text
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import net.liftweb.util.{FieldIdentifier, FieldError}
import queue.ApplicationEvent

class ResourceOwner extends MongoModel[ResourceOwner] with MongoId[ResourceOwner] with Timestamps[ResourceOwner] {

  /**
   * The companion object of this user
   */
  def meta = ResourceOwner

  /**
   * The login name in use by this user
   */
  object login extends RequiredStringField(this, 100)  {
    override def validations = validateUnique _ ::
        validateRegex("""^\w*$""".r, " can only contain letters, digits and underscores") _ ::
        super.validations

    override def setFilter = toLower _ :: super.setFilter
  }

  /**
   * The email address of this user
   */
  object email extends RequiredEmailField(this, 150)  {
    override def setFilter = toLower _ :: super.setFilter
    override def validations = validateUnique _ :: super.validations
  }

  object confirmationSentAt extends JodaDateTimeField(this) {
    override def defaultValue = MIN_DATE
  }
  object confirmationToken extends TokenField(this)
  object confirmedAt extends JodaDateTimeField(this) {
    override def defaultValue = MIN_DATE
  }

  def confirmed_? = confirmedAt.value != confirmedAt.defaultValue

  object resetSentAt extends JodaDateTimeField(this) {
    override def defaultValue = MIN_DATE
  }
  object resetToken extends TokenField(this)
  object resetAt extends JodaDateTimeField(this) {
    override def defaultValue = MIN_DATE
  }

  def reset_? = resetAt.value != resetAt.defaultValue

  object currentSignInAt extends JodaDateTimeField(this) {
    override def defaultValue = MIN_DATE
  }
  object currentSignInIp extends StringField(this, 15)
  object lastSignInAt extends JodaDateTimeField(this) {
    override def defaultValue = MIN_DATE
  }
  object lastSignInIp extends StringField(this, 15)
  object rememberCreatedAt extends JodaDateTimeField(this) {
    override def defaultValue = MIN_DATE
  }
  object rememberToken extends TokenField(this)

  /**
   * The first name for this user
   */
  object firstName extends StringField(this, 160)

  /**
   * The last name for this user
   */
  object lastName extends StringField(this, 160)

  object signInCount extends IntField(this)

  object password extends BCryptPasswordField(this, 6)

  def fullName = "%s %s" format (firstName.value, lastName.value)

  def update(fn: String, ln: String,
             em: String) = {
    firstName set fn
    lastName set ln

    var errors = firstName.validate ::: lastName.validate

    if (em != email.value) {
      email set em
      errors = email.validate ::: errors
    }

    if(errors.isEmpty) {
      save
      Right(this)
    } else {
      Left(errorsAsJson(errors))
    }
  }

  def updatePassword(pwd: String, passwordConfirmation: String) = {
    password.setPassword(pwd)
    var errors = password.validate
    if (pwd != passwordConfirmation)
      errors = FieldError(password, "Password confirmation does not match") +: errors

    if(errors.isEmpty) {
      save
      Right(this)
    } else {
      Left(errorsAsJson(errors))
    }
  }

  def tickOnSignIn(remoteAddress: String) {
    lastSignInAt set currentSignInAt.value
    currentSignInAt set DateTime.now
    lastSignInIp set currentSignInIp.value
    currentSignInIp set remoteAddress
    signInCount set (signInCount.value + 1)
  }
}

object ResourceOwner extends ResourceOwner with MongoMetaModel[ResourceOwner] with Logging {
  override def collectionName = "users"

  ensureIndex(("login" -> 1), true)
  ensureIndex(("email" -> 1), true)
  ensureIndex(("confirmation_token.token" -> 1), ("background" -> 1) ~ ("unique" -> 1))
  ensureIndex(("remember_token.token" -> 1), ("background" -> 1) ~ ("unique" -> 1))

  def findByLoginOrEmail(loginOrEmail: String) = {
    val l = loginOrEmail.toLowerCase(ENGLISH)
    find($or("login" -> l, "email" -> l))
  }

  def login(login: String, password: String, remoteAddress: String): Box[ResourceOwner] = {
    findByLoginOrEmail(login) flatMap { user =>
      if(user.confirmed_? && (user.password matches_? password)) {
        user tickOnSignIn remoteAddress
        user.save
        Full(user)
      } else Empty
    }
  }

  def signup(fn: String, ln: String,
             login: String, email: String,
             password: String, passwordConfirmation: String,
             plan: String,
             terms: Boolean) = {

    val user = ResourceOwner.createRecord
    user.firstName.set(fn)
    user.lastName.set(ln)
    user.login.set(login)
    user.email.set(email)
    user.confirmationToken.refreshToken
    user.confirmationSentAt.set(DateTime.now)
    user.password.setPassword(password)

    var errors = user.validate

    if (password != passwordConfirmation)
      errors = FieldError(user.password, "Password confirmation does not match") +: errors

    if (terms && errors.isEmpty) {
      user.save
      MessageQueue.publishEvent("backchat:send:email", Confirmation(user, 'confirm, user.confirmationToken.value.token))
      Right(user)
    } else {
      var json = errorsAsJson(errors)
      if (!terms)
        json = JObject(JField("terms", "User needs to accept terms") :: Nil) +: json
      Left(json)
    }
  }

  def confirmRegistration(token: String) = {
    if (token.isEmpty)
      Left(List(JObject(JField("token", "The token is missing") :: Nil)))
    else {
      find("confirmation_token.token" -> token) match {
        case Full(usr) =>
          if (!usr.confirmed_?) {
            usr.confirmedAt.set(DateTime.now)
            usr.save
            Right(usr)
          } else
            Left(List(JObject(JField("token", "Invalid token.") :: Nil)))
        case _ =>
          Left(List(JObject(JField("token", "Invalid token") :: Nil)))
      }
    }
  }

  def forgotPassword(login: String) = {
    findByLoginOrEmail(login) match {
      case Full(usr) =>
        usr.resetToken.refreshToken
        usr.resetSentAt.set(DateTime.now)
        usr.resetAt.set(resetAt.defaultValue)
        usr.save
        MessageQueue.publishEvent("backchat:send:email", Confirmation(usr, 'reset, usr.resetToken.value.token))
        Right(usr)
      case _ =>
        Left(List(JObject(JField("login", "User not found") :: Nil)))
    }
  }

  def resetPassword(token: String, password: String, passwordConfirmation: String) = {
    if (token.isEmpty)
      Left(List(JObject(JField("token", "The token is missing.") :: Nil)))
    else {
      if (password != passwordConfirmation)
        Left(List(JObject(JField("password", "Password confirmation does not match.") :: Nil)))
      else {
        find("reset_token.token" -> token) match {
          case Full(usr) =>
            if (!usr.reset_?) {
              usr.password.setPassword(password)
              val errors = usr.password.validate
              if (errors.isEmpty) {
                usr.resetAt.set(DateTime.now)
                usr.save
                Right(usr)
              } else
                errorsAsLeft(errors:_*)
            } else
              Left(List(JObject(JField("token", "Invalid token.") :: Nil)))
          case _ =>
            Left(List(JObject(JField("token", "Invalid token.") :: Nil)))
        }
      }
    }
  }

  object Confirmation {
    def apply(user: ResourceOwner, event: Symbol, token: String) = ApplicationEvent(
      event,
      ("name" -> user.fullName) ~
      ("email" -> user.email.value) ~
      ("token" -> token))
  }
}

