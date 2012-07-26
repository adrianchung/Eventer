package controllers

import play.api._
import libs.json.{JsValue, Json, JsObject}
import play.api.mvc._
import models.Event
import org.bson.types.ObjectId
import java.util.Date
import java.text._
import com.codahale.jerkson.Json._
import play.mvc.Http.Context
import scala.collection.JavaConverters._


/**
 * Controller for handling all of the events
 */
object EventsApi extends Controller {

  /**
   * Get all of the events currently, NOT SCALABLE
   * @return All events available
   */
  def getAllEvents() = Action { implicit request =>
    val events = Event.findAll()

    Ok(Json.toJson(events.map( event => Event.toJsonEvent(event) ).toSeq))
  }

  /**
   * Get all of the events that a user is attending, or created
   * @param userId The user id to retrieve events for
   * @return Events that belong to or is created by the user
   */
  def getEvents(userId: ObjectId) = Action { implicit request =>
    val events = Event.findAllByUserId(userId)
    val collectedEvents = events.map { event =>
      event
    }

    val eventsImAttending = Event.findEventsAttending(userId)
    val eventsImAttendingSeq = eventsImAttending.map { event =>
      event
    }

    // Combine the two iterators and convert to a sequence of JsValue
    val allEvents = (collectedEvents ++ eventsImAttendingSeq).toSeq

    // Remove duplicated events as per
    // http://stackoverflow.com/questions/3912753/scala-remove-duplicates-in-list-of-objects
    val filteredEvents = allEvents.groupBy{_.id}.map{_._2.head}
    val mappedFilteredEvents = filteredEvents.map { event => Event.toJsonEvent(event) }.toSeq

    Ok(Json.toJson(mappedFilteredEvents))
  }

  /**
   * Get a specific event by its id
   * @param userId Used for authentication purposes, not currently required
   * @param eventId The event id to fetch
   * @return The details of the event
   */
  def getEventById(userId: ObjectId, eventId: ObjectId) = Action { implicit request =>
    Event.findOneById(eventId).map( event =>
      Ok(Event.toJsonEvent(event))
    ).getOrElse(Ok(views.html.index("NotFound", eventId + " was not found")))
  }

  /**
   * Create an event for a user
   * @param userId The user id to create the event for
   * @return The created id of the event
   */
  def createEvent(userId: ObjectId) = Action(parse.json) { implicit request =>
    
    val event = createEventFromBody(new ObjectId(), userId, request.body)

    Event.save(event);
    Ok(Json.toJson(
      Map(
        "success" -> Json.toJson(true),
        "id" -> Json.toJson(event.id.toString()),
        "version" -> Json.toJson(event.version)
      )
    ))
  }

  /**
   * Delete an event
   * TODO we don't check permissions at all to see if the user owns the event
   * @param userId The user id the event belongs to
   * @param eventId The event id to delete
   * @return The result of the delete operation
   */
  def deleteEvent(userId: ObjectId, eventId: ObjectId) = Action { implicit request =>
    Event.removeById(eventId);
    Ok(Json.toJson(
      Map(
        "success" -> Json.toJson(true),
        "id" -> Json.toJson(eventId.toString())
      )
    ))
  }

  /**
   * Update details of an event
   * @param userId The user id of the event owner
   * @param eventId The event id to update
   * @return The result of the update operation
   */
  def updateEvent(userId: ObjectId, eventId: ObjectId) = Action(parse.json) { implicit request =>
    val event = createEventFromBody(eventId, userId, request.body)

    val returnValue = Event.updateVersionAndSave(event)
    if (returnValue._1 == 0) {
      Ok(Json.toJson(
        Map(
          "success" -> Json.toJson(true),
          "id" -> Json.toJson(event.id.toString()),
          "version" -> Json.toJson(returnValue._4)
        )
      ))
    } else {
      Ok(Json.toJson(
        Map(
          "success" -> Json.toJson(false),
          "id" -> Json.toJson(event.id.toString()),
          "error_code" -> Json.toJson(returnValue._2),
          "message" -> Json.toJson(returnValue._3)
        )
      ))
    }
  }

  /**
   * Add a user id as an attendee to the given event
   * @param userId The user id that will be joining the event
   * @param eventId The event id to join
   * @return The result of the join operation
   */
  def joinEvent(userId: ObjectId, eventId: ObjectId) = Action { implicit request =>
    Event.findOneById(eventId).map({ event =>
      val returnValue = Event.addAttendeeAndSave(userId, event)
      if (returnValue._1 == 0) {
        Ok(Json.toJson(
          Map(
            "success" -> Json.toJson(true),
            "id" -> Json.toJson(event.id.toString()),
            "version" -> Json.toJson(returnValue._4)
          )
        ))
      } else {
        Ok(Json.toJson(
          Map(
            "success" -> Json.toJson(false),
            "id" -> Json.toJson(event.id.toString()),
            "error_code" -> Json.toJson(returnValue._2),
            "message" -> Json.toJson(returnValue._3)
          )
        ))
      }
    }).getOrElse(Ok(views.html.index("NotFound", eventId + " was not found")))
  }

  /**
   * Remove a user as an attendee from an event
   * @param userId The user id to remove from the event
   * @param eventId The event id to remove the user from
   * @return The result of the remove operation
   */
  def leaveEvent(userId: ObjectId, eventId: ObjectId) = Action { implicit request =>
    Event.findOneById(eventId).map({ event =>
      val returnValue = Event.removeAttendeeAndSave(userId, event)
      if (returnValue._1 == 0) {
        Ok(Json.toJson(
          Map(
            "success" -> Json.toJson(true),
            "id" -> Json.toJson(event.id.toString()),
            "version" -> Json.toJson(returnValue._4)
          )
        ))
      } else {
        Ok(Json.toJson(
          Map(
            "success" -> Json.toJson(false),
            "id" -> Json.toJson(event.id.toString()),
            "error_code" -> Json.toJson(returnValue._2),
            "message" -> Json.toJson(returnValue._3)
          )
        ))
      }
    }).getOrElse(Ok(views.html.index("NotFound", eventId + " was not found")))
  }

  def createEventFromBody(eventId: ObjectId, userId: ObjectId, requestBody: JsValue) : Event = {
    val createdBy = userId;

    val subject = (requestBody \ "subject").asOpt[String].map { subject =>
      subject
    }.getOrElse {
      BadRequest("Missing parameter [subject]")
    }

    val body = (requestBody \ "body").asOpt[String].map { body =>
      body
    }.getOrElse {
      BadRequest("Missing parameter [body]")
    }

    val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    val startBody = (requestBody \ "start").asOpt[String].map { start =>
      start
    }.getOrElse {
      BadRequest("Missing parameter [start]")
    }

    val endBody = (requestBody \ "end").asOpt[String].map { end =>
      end
    }.getOrElse {
      BadRequest("Missing parameter [end]")
    }
    val start:Date = dateFormat.parse(startBody.toString())
    val end:Date = dateFormat.parse(endBody.toString())

    val location = (requestBody \ "location").asOpt[String].map { location =>
      location
    }.getOrElse {
      BadRequest("Missing parameter [location]")
    }

    val image = (requestBody \ "image").asOpt[String].map { image =>
      image
    }.getOrElse {
      BadRequest("Missing parameter [image]")
    }

    val version = (requestBody \ "version").asOpt[Int].map { version =>
      version
    }.getOrElse {
      1
    }

    // Special this is a list
    val attendees = (requestBody \\ "attendees").map { attendee =>
      val listConvert = attendee.as[List[String]]
      val convertedList = listConvert.map(str => new ObjectId(str))
      convertedList
    }

    new Event(
      id = eventId,
      createdBy = createdBy,
      subject = subject.toString(),
      body = body.toString(),
      start = start,
      end = end,
      location = location.toString(),
      attendees = attendees.apply(0),
      image = image.toString(),
      version = version
    )
  }
}
