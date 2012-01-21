package org.scalatra
package oauth2
package model

import mongo.{MongoMetaModel, MongoModel}
import net.liftweb.mongodb.record.MongoId
import mongo.fields.SharedFields.Timestamps
import net.liftweb.mongodb.record.field.MongoListField
import model.Enums._
import mongo.fields.{JodaDateTimeField, TokenField}
import net.liftweb.record.field.{IntField, EnumNameField, OptionalStringField, StringField}

/**
 * Client. This an entity a resource owner can grant access to. (an application on twitter for example is a client)
 */
class Client extends MongoModel[Client] with MongoId[Client] with Timestamps[Client] {
  def meta = Client

  /** Client secret: random, long, and hexy */
  object secret extends StringField(this, 32)
  /** ResourceOwner see this. */
  object displayName extends StringField(this, 150)
  /** Link to client's Web site. */
  object link extends OptionalStringField(this, 1024)
  /** Redirect URL. Supplied by the client if they want to restrict redirect */
  object redirectUri extends OptionalStringField(this, 1024)
  /** Preferred image URL for this client's icon. */
  object imageUrl extends OptionalStringField(this, 1024)
  /** URLs (better security). */
  object urlWhitelist extends MongoListField[Client, String](this)
  /** List of scope the client is allowed to request. */
  object scope extends MongoListField[Client, String](this)
  /** Free form fields for internal use. */
  object notes extends StringField(this, 1024)
  /** The authorization type this client is allowed to use */
  object authorizationType extends EnumNameField(this, AuthorizationType)
  /** Timestamp if revoked. */
  object revoked extends JodaDateTimeField(this) {
    override def defaultValue = MIN_DATE
  }
  /** Counts how many access tokens were granted. */
  object tokensGranted extends IntField(this)
  /** Counts how many access tokens were revoked. */
  object tokensRevoked extends IntField(this)

  def revoked_? = (revoked.valueBox openOr MIN_DATE) > MIN_DATE
}

object Client extends Client with MongoMetaModel[Client] {

}