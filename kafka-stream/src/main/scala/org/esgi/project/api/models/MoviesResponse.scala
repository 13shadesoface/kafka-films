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

case class ViewList(
    views: List[ViewCount]
)

object ViewList {
  implicit val format: OFormat[ViewList] = Json.format[ViewList]
}

case class ViewCount(
    id: Int,
    title: String,
    views: Long
)

object ViewCount {
  implicit val format: OFormat[ViewCount] = Json.format[ViewCount]
}

case class LikeCount(
                      id: Int,
                      title: String,
                      scores: Double
                    )

case class LikeList(
    views: List[LikeCount]
)

object LikeList {
  implicit val format: OFormat[LikeList] = Json.format[LikeList]
}


object LikeCount {
  implicit val format: OFormat[LikeCount] = Json.format[LikeCount]
}
