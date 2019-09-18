package utils

import java.util.UUID

import javax.inject.Singleton
import org.joda.time.DateTime

@Singleton
class GenericToolset {

  def randomUUID(): UUID ={
    UUID.randomUUID
  }

  def currentTime(): DateTime ={
    DateTime.now
  }
}
