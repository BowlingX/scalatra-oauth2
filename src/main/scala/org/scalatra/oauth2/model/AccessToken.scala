package org.scalatra
package oauth2
package model

import net.liftweb.mongodb.record.MongoId
import mojolly.mongo.fields.SharedFields.Timestamps
import mongo.{MongoMetaModel, MongoModel}
import net.liftweb.record.field.StringField
import net.liftweb.mongodb.record.field.MongoListField
import mongo.fields.JodaDateTimeField
import net.liftweb.json.JsonDSL._

/**
 *  Access token. This is what clients use to access resources.
 *
 *  An access token is a unique code, associated with a client, an identity
 *  and scope. It may be revoked, or expire after a certain period.
 */
class AccessToken extends MongoModel[AccessToken] with MongoId[AccessToken] with Timestamps[AccessToken] {
  def meta = AccessToken

  /** Access token. As unique as they come. */
  object token extends StringField(this, 32)

  /** The identity we authorized access to. */
  object identity extends StringField(this, 32)

  /** Client that was granted this access token. */
  object clientId extends StringField(this, 32)

  /** The scope granted to this token. */
  object scope extends MongoListField[AccessToken, String](this)

  /** When token expires for good. */
  object expiresAt extends JodaDateTimeField(this) {
    override def defaultValue = MIN_DATE
  }

  /** Timestamp if revoked. */
  object revoked extends JodaDateTimeField(this) {
    override def defaultValue = MIN_DATE
  }

  /** Timestamp of last access using this token, rounded up to hour. */
  object lastAccess extends JodaDateTimeField(this)

  /** Timestamp of previous access using this token, rounded up to hour. */
  object previousAccess extends JodaDateTimeField(this) {
    override def defaultValue = MIN_DATE
  }

  def revoked_? = (revoked.valueBox openOr MIN_DATE) > MIN_DATE
  def expired_? = {
    val d = (expiresAt.valueBox openOr MIN_DATE)
    d > MIN_DATE && d < DateTime.now.toDateMidnight
  }

  def tickAccess = {
    previousAccess set lastAccess.value
    lastAccess set DateTime.now
  }

}

object AccessToken extends AccessToken with MongoMetaModel[AccessToken] {

  ensureIndex(("token" -> 1), true)
}