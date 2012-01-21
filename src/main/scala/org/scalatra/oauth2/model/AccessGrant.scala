package org.scalatra
package oauth2
package model

import mojolly.mongo.fields.SharedFields.Timestamps
import net.liftweb.mongodb.record.MongoId
import mojolly.mongo.{MongoModel, MongoMetaModel}
import net.liftweb.record.field.StringField
import net.liftweb.mongodb.record.field.MongoListField
import mongo.fields.JodaDateTimeField


class AccessGrant extends MongoModel[AccessGrant] with MongoId[AccessGrant] with Timestamps[AccessGrant] {
  def meta = AccessGrant

  /** Authorization code. We are nothing without it. */
  object code extends StringField(this, 32)
  /** The identity we authorized access to. */
  object identity extends StringField(this, 32)
  /** Client that was granted this access token. */
  object clientId extends StringField(this, 32)
  /** Redirect URI for this grant. */
  object redirectUri extends StringField(this, 1024)
  /** The scope requested in this grant. */
  object scope extends MongoListField[AccessGrant, String](this)

  /** Tells us when (and if) access token was created. */
  object grantedAt extends JodaDateTimeField(this) {
    override def defaultValue = MIN_DATE
  }
  /** Tells us when this grant expires. */
  object expiresAt extends JodaDateTimeField(this) {
    override def defaultValue = MIN_DATE
  }

  /** Access token created from this grant. Set and spent. */
  object accessToken extends StringField(this, 32)

  /** Timestamp if revoked. */
  object revoked extends JodaDateTimeField(this) {
    override def defaultValue = MIN_DATE
  }

  def revoked_? = (revoked.valueBox openOr MIN_DATE) > MIN_DATE
  def expired_? = {
    val d = (expiresAt.valueBox openOr MIN_DATE)
    d > MIN_DATE && d < DateTime.now.toDateMidnight
  }
  def granted_? = (grantedAt.valueBox openOr MIN_DATE) > MIN_DATE
}
object AccessGrant extends AccessGrant with MongoMetaModel[AccessGrant] {

}