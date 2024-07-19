package org.esgi.project.streaming.models

import play.api.libs.json.{Json, OFormat}

case class ViewCountWithTitle(
    count: Long,
    title: String
)

object ViewCountWithTitle {
  implicit val format: OFormat[ViewCountWithTitle] = Json.format[ViewCountWithTitle]
}
