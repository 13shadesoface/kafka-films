package org.esgi.project.api.models

import play.api.libs.json.{Json, OFormat}

case class MoviesResponse(
    id: Int,
    title: String,
    total_view_count: Long,
    stats: ViewStats
)

object MoviesResponse {
  implicit val format: OFormat[MoviesResponse] = Json.format[MoviesResponse]
}
