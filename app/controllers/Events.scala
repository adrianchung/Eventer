package controllers

import org.bson.types.ObjectId
import models.Event
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._


object Events extends Controller {
  def index() = TODO

  def getEvents(userId: ObjectId) = Action { implicit request =>
    val events = Event.findAllByUserId(userId)
    // Combine the two sequences
    //events :+ Event.findEventsAttending(userId)

    Ok(views.html.user_index(userId, events))
  }

  def getEventById(userId: ObjectId, eventId: ObjectId) = Action { implicit request =>
    Event.findOneById(eventId).map( event =>
      Ok(views.html.event(userId, event))
    ).getOrElse(Ok(views.html.index("NotFound", eventId + " was not found")))
  }

  /**
   * Create event form.
   *
   * Once defined it handle automatically, ,
   * validation, submission, errors, redisplaying, ...
   */
  /*val createEventForm: Form[Event] = Form(

    // Define a mapping that will handle Event values
    mapping(
      "subject" -> nonEmptyText,
      "body" -> nonEmptyText,
      "start" -> nonEmptyText,
      "end" -> nonEmptyText,
      "location" -> nonEmptyText,
      "attendees" -> nonEmptyText,
      "image" -> nonEmptyText,
      "id" -> nonEmptyText,
      "createdBy" -> nonEmptyText
    )*/
}
