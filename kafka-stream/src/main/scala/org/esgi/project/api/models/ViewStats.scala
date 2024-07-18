package org.esgi.project.api.models

import play.api.libs.json.{Json, OFormat}

case class ViewStats(
                      past: Map[String, Long],
                      last_five_minutes: Map[String, Long]
                     )


object ViewStats {
  implicit val format: OFormat[ViewStats] = Json.format[ViewStats]
}
