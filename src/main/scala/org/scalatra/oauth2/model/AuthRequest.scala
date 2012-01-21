package org.scalatra
package oauth2
package model

import mongo.{MongoMetaModel, MongoModel}
import net.liftweb.mongodb.record.field.MongoListField
import mongo.fields.SharedFields.Timestamps
import net.liftweb.mongodb.record.MongoId
import Enums._
import net.liftweb.record.field.{StringField, EnumNameField, OptionalEnumNameField, OptionalStringField}
import mongo.fields.{JodaDateTimeField, RequiredStringField}

/**
 * Authorization request. Represents request on behalf of client to access
 * particular scope. Use this to keep state from incoming authorization
 * request to grant/deny redirect.
 */
class AuthRequest extends MongoModel[AuthRequest] with MongoId[AuthRequest] with Timestamps[AuthRequest] {
  def meta = AuthRequest

  /** Client making request */
  object clientId extends RequiredStringField(this, 32)

  /** scope of this request */
  object scope extends MongoListField[AuthRequest, String](this)

  /** redirect back to this URI */
  object redirectUri extends RequiredStringField(this, 1024)
  /** Client requested we return state on redirect. */
  object state extends OptionalStringField(this, 50)

  /** Response type: either code or token. */
  object responseType extends OptionalEnumNameField(this, ResponseType)

  /** If granted, the access grant code. */
  object grantCode extends EnumNameField(this, GrantCode)

  /** If granted, the access token. */
  object accessToken extends StringField(this, 32)

  /** Keeping track of things. */
  object authorizedAt extends JodaDateTimeField(this) {
    override def defaultValue = MIN_DATE
  }

  /** Timestamp if revoked. */
  object revoked extends JodaDateTimeField(this) {
    override def defaultValue = MIN_DATE
  }

  def revoked_? = (revoked.valueBox openOr MIN_DATE) > MIN_DATE
  def authorized_? = (authorizedAt.valueBox openOr MIN_DATE) > MIN_DATE

}

/**
 * Authorization request. Represents request on behalf of client to access
 * particular scope. Use this to keep state from incoming authorization
 * request to grant/deny redirect.
 */
object AuthRequest extends AuthRequest with MongoMetaModel[AuthRequest] {

}