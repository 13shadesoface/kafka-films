package org.esgi.project.streaming.models

import play.api.libs.json.{Json, OFormat}

case class MeanScoreforLike(
    sum: Double,
    count: Double,
    meanScore: Double
) {
  def increment(score: Double) = this.copy(sum = this.sum + score, count = this.count + 1).computeMeanScore

  def computeMeanScore = this.copy(
    meanScore = this.sum / this.count
  )
}

object MeanScoreforLike {
  implicit val format: OFormat[MeanScoreforLike] = Json.format[MeanScoreforLike]

  def empty: MeanScoreforLike = MeanScoreforLike(0, 0, 0)
}

