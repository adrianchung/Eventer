package models

import org.bson.types.ObjectId;

import play.api.Play.current
import java.util.Date
import com.novus.salat.annotations._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.mongoContext._
import play.api.libs.json.{JsValue, Json}
import java.text._

case class Event(id: ObjectId = new ObjectId,
                 createdBy: ObjectId,
                 subject: String,
                 body: String,
                 start: Date,
                 end: Date,
                 location: String,
                 attendees: List[ObjectId],
                 image: String,
                 version: Int = 1
    )

object Event extends ModelCompanion[Event, ObjectId] {
  val collection = mongoCollection("events")
  val dao = new SalatDAO[Event, ObjectId](collection = collection) {}

  def findAllByUserId(userId: ObjectId) = dao.find(MongoDBObject("createdBy" -> userId))
  def findEventById(eventId: ObjectId): Option[Event] = dao.findOne(MongoDBObject("_id" -> eventId))
  def findEventsAttending(userId: ObjectId) = dao.find(MongoDBObject("attendees" -> userId))

  def addAttendeeAndSave(userId: ObjectId, event: Event): (Int, String, String, Int) = {
    if (event.attendees.contains(userId)) {
      return (1002, "USER_ALREADY_ATTENDING", "The user is already attending this event", -1)
    }

    val toSaveEvent = new Event(
      id = event.id,
      createdBy = event.createdBy,
      subject = event.subject,
      body = event.body,
      start = event.start,
      end = event.end,
      location = event.location,
      attendees = event.attendees :+ userId,
      image = event.image,
      version = event.version
    )

    updateVersionAndSave(toSaveEvent)
  }

  def removeAttendeeAndSave(userId: ObjectId, event: Event): (Int, String, String, Int) = {
    val toSaveEvent = new Event(
      id = event.id,
      createdBy = event.createdBy,
      subject = event.subject,
      body = event.body,
      start = event.start,
      end = event.end,
      location = event.location,
      attendees = event.attendees.filter( objId => objId != userId ),
      image = event.image,
      version = event.version
    )

    updateVersionAndSave(toSaveEvent)
  }

  def updateVersionAndSave(event: Event): (Int, String, String, Int) = {
    val foundEvent = findEventById(event.id)

    // Need to check the version marks here and return error if they mismatch
    if (foundEvent.isDefined) {
      val existingEvent = foundEvent.get
      if (event.version < existingEvent.version) {
        return (1001, "EVENT_OUT_OF_SYNC", "Event versions do not match", -1)
      }
    }

    val toSaveEvent = new Event(
      id = event.id,
      createdBy = event.createdBy,
      subject = event.subject,
      body = event.body,
      start = event.start,
      end = event.end,
      location = event.location,
      attendees = event.attendees,
      image = event.image,
      version = event.version + 1
    )

    save(toSaveEvent)
    (0, "", "", toSaveEvent.version)
  }

  def toJsonEvent(event: Event): JsValue = {
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    Json.toJson(
    Map(
        "id" -> Json.toJson(event.id.toString()),
        "created_by" -> Json.toJson(event.createdBy.toString()),
        "subject" -> Json.toJson(event.subject),
        "start" -> Json.toJson(dateFormat.format(event.start)),
        "end" -> Json.toJson(dateFormat.format(event.end)),
        "body" -> Json.toJson(event.body),
        "location" -> Json.toJson(event.location),
        "image" -> Json.toJson(event.image),
        "attendees" -> Json.toJson(event.attendees.map { objectId => objectId.toString() }),
        "version" -> Json.toJson(event.version)
      )
    )
  }
}
