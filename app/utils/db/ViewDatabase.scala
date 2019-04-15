package utils.db

import cats.effect.IO
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
  def viewVersionNonExistAction(event: Event)
    (action: String => IO[Unit])
    (implicit executionContext: ExecutionContext): IO[Unit] = IO {
    event.data.fold(logger.warn(s"[$event] is None")) { eventData =>
      val targetVersion = (eventData \ "version").as[String]
      eventDao.findByType(event.`type`).map { storedEvents =>
        val targetVersionCount = storedEvents.count { storedEvent =>
          storedEvent.data.fold(false) { storedEventData =>
            val eventVersion = (storedEventData \ "version").as[String]
            targetVersion == eventVersion
          }
        }

        if (targetVersionCount == 1) {
          action.apply(targetVersion)
        } else {
          logger.warn(s"View version $targetVersion already exists")
        }
      }
    }
  }
}
