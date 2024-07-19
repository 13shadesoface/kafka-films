package org.esgi.project.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.apache.kafka.streams.{KafkaStreams, StoreQueryParameters}
import org.apache.kafka.streams.state.{
  QueryableStoreTypes,
  ReadOnlyKeyValueStore,
  ReadOnlyWindowStore,
  WindowStore,
  WindowStoreIterator
}
import org.esgi.project.api.models.{MeanLatencyForURLResponse, MoviesResponse, ViewStats, VisitCountResponse}
import org.esgi.project.streaming.StreamProcessing
import org.esgi.project.streaming.models.{MeanScoreforLike, ViewCountWithTitle}

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.jdk.CollectionConverters._

object WebServer extends PlayJsonSupport {
  def getMovieStats(
      id: Int,
      viewCountStore: ReadOnlyKeyValueStore[String, ViewCountWithTitle],
      viewCountWindowedStore: ReadOnlyWindowStore[String, Long]
  ): MoviesResponse = {
    val keyPrefix = id.toString

    def getViewCount(suffix: String): Long =
      Option(viewCountStore.get(s"$keyPrefix-$suffix")).map(_.count).getOrElse(0L)

    def getViewTitle(suffix: String): String = {
      val categories = Seq("start_only", "half", "full")
      categories
        .collectFirst {
          case suffix if Option(viewCountStore.get(s"$keyPrefix-$suffix")).exists(_.title.nonEmpty) =>
            viewCountStore.get(s"$keyPrefix-$suffix").title
        }
        .getOrElse("")
    }

    val totalViewCount = getViewCount("start_only") + getViewCount("half") + getViewCount("full")
    val title = getViewTitle("start_only")
    val pastStats = Map(
      "start_only" -> getViewCount("start_only"),
      "half" -> getViewCount("half"),
      "full" -> getViewCount("full")
    )

    val currentTime = Instant.now()
    val windowStartTime = currentTime.minus(Duration.ofMinutes(5))

    def fetchWindowedResults(
        store: ReadOnlyWindowStore[String, Long],
        key: String,
        from: Instant,
        to: Instant
    ): Long = {
      val iterator = store.fetch(key, from, to)
      iterator.asScala.foldLeft(0L)((agg, kv) => agg + kv.value)
    }

    val lastFiveMinutesStats = Map(
      "start_only" -> fetchWindowedResults(
        viewCountWindowedStore,
        s"$keyPrefix-start_only",
        windowStartTime,
        currentTime
      ),
      "half" -> fetchWindowedResults(viewCountWindowedStore, s"$keyPrefix-half", windowStartTime, currentTime),
      "full" -> fetchWindowedResults(viewCountWindowedStore, s"$keyPrefix-full", windowStartTime, currentTime)
    )

    MoviesResponse(id, title, totalViewCount, ViewStats(pastStats, lastFiveMinutesStats))
  }

  def routes(streams: KafkaStreams): Route = {
    val viewCountStore: ReadOnlyKeyValueStore[String, ViewCountWithTitle] =
      streams.store(
        StoreQueryParameters.fromNameAndType(
          StreamProcessing.viewCountByIdCategoryStorename,
          QueryableStoreTypes.keyValueStore[String, ViewCountWithTitle]
        )
      )

    val viewCountWindowedStore: ReadOnlyWindowStore[String, Long] =
      streams.store(
        StoreQueryParameters.fromNameAndType(
          StreamProcessing.viewCountByWindowedIdAndCategoryStore,
          QueryableStoreTypes.windowStore[String, Long]
        )
      )

//    val highScoreStore: ReadOnlyKeyValueStore[Int, MeanScoreforLike] =
//      streams.store("high-score-store", QueryableStoreTypes.keyValueStore[Int, MeanScoreforLike])
//
//    val lowScoreStore: ReadOnlyKeyValueStore[Int, MeanScoreforLike] =
//      streams.store("low-score-store", QueryableStoreTypes.keyValueStore[Int, MeanScoreforLike])
//
//    val avgScoreStore: ReadOnlyKeyValueStore[Int, MeanScoreforLike] =
//      streams.store("avg-score-store", QueryableStoreTypes.keyValueStore[Int, MeanScoreforLike])
    concat(
      path("movies" / IntNumber) { id: Int =>
        get {
          complete(
            getMovieStats(id, viewCountStore, viewCountWindowedStore)
          )
        }
      }
//      path("stats" / "ten" / "best" / "score") {
//        get {
//          complete(
//            getTopTenBestScores()
//          )
//        }
//      },
//      path("stats" / "ten" / "worse" / "score") {
//        get {
//          complete(
//            getTopTenBestScores()
//          )
//        }
//      },
//      path("stats" / "ten" / "best" / "views") {
//        get {
//          complete(
//            getTopTenBestScores()
//          )
//        }
//      },
//      path("stats" / "ten" / "worse" / "views") {
//        get {
//          complete(
//            getTopTenBestScores()
//          )
//        }
//      }
    )
  }
}
