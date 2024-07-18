package streaming.models

import play.api.libs.json.{Json, OFormat}

case class MeanScoreforLike(
                              sum: Long,
                              count: Long,
                              meanLatency: Long
                            ) {
  def increment(latency: Long) = this.copy(sum = this.sum + latency, count = this.count + 1).computeMeanScore

  def computeMeanScore = this.copy(
    meanLatency = this.sum / this.count
  )
}

object MeanScoreforLike {
  implicit val format: OFormat[MeanScoreforLike] = Json.format[MeanScoreforLike]

  def empty: MeanScoreforLike = MeanScoreforLike(0, 0, 0)
}
