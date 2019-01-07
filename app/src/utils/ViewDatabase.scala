package src.utils

import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import src.{Event, EventDao}
import scala.concurrent.ExecutionContext

/**
  * This object contains all the utilities for view database
  */
@Singleton
class ViewDatabase @Inject()(eventDao: EventDao) extends LazyLogging {

  /**
    * The view database schema evolves overtime
    * This function handles the evolution of schema by feeding the versioning definition and incoming version data from event
    *
    * @param event The incoming event specifying what version of schema to apply
    * @param versioning The versioning schema that needs to be fed in
    *                   Typically table creation, alteration and deletion defined per version definition
    *                   This will be defined in each View's DAO
    */
  def evolveViewSchema(
    event: Event
  )(versioning: String => Any)(implicit executionContext: ExecutionContext) = {
    val targetVersion = (event.data.get \ "version").as[String]
    eventDao
      .findByType(event.`type`)
      .map { events =>
        val targetVersionExists = events
          .exists { event =>
            val eventVersion = (event.data.get \ "version").as[String]
            targetVersion == eventVersion
          }
        if (!targetVersionExists) {
          versioning.apply(targetVersion)
        } else {
          logger.warn(s"Version $targetVersion already exists")
        }
      }
  }
}
