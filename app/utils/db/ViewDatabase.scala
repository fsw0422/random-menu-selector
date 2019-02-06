package utils.db

import com.typesafe.scalalogging.LazyLogging
import event.{Event, EventDao}
import javax.inject.{Inject, Singleton}

import scala.concurrent.ExecutionContext

/**
  * This object contains all the utilities for view database
  */
@Singleton
class ViewDatabase @Inject()(eventDao: EventDao) extends LazyLogging {

  /**
    * This function handles the action after checking if the view datavase of the specified version exists
    *
    * @param event The incoming event specifying what version of schema to apply
    * @param action The action that needs to be fed
    */
  def viewVersionNonExistAction(
    event: Event
  )(action: String => Any)(implicit executionContext: ExecutionContext) = {
    val targetVersion = (event.data.get \ "version").as[String]
    eventDao
      .findByType(event.`type`)
      .map { events =>
        val targetVersionCount = events
          .count { event =>
            val eventVersion = (event.data.get \ "version").as[String]
            targetVersion == eventVersion
          }
        if (targetVersionCount == 1) {
          action.apply(targetVersion)
        } else {
          logger.warn(s"View version $targetVersion already exists")
        }
      }
  }
}
