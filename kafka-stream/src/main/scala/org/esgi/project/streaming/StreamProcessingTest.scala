package org.esgi.project.streaming

import io.github.azhur.kafka.serde.PlayJsonSupport
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.streams.kstream.{JoinWindows, TimeWindows, Windowed}
import org.apache.kafka.streams.scala._
import org.apache.kafka.streams.scala.kstream.{KGroupedStream, KTable, Materialized}
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig, Topology}
import org.esgi.project.api.models.{Like, View}
import org.esgi.project.streaming.models.{MeanScoreforLike, ViewCountWithTitle}

import java.util.Properties
import java.time.Instant
import java.time.Duration

object StreamProcessingTest extends PlayJsonSupport {

  import org.apache.kafka.streams.scala.ImplicitConversions._
  import org.apache.kafka.streams.scala.serialization.Serdes._

  val applicationName = s"some-application-name${Instant.now().toEpochMilli}"

  private val props: Properties = buildProperties

  // defining processing graph
  val builder: StreamsBuilder = new StreamsBuilder

  val viewTopic = "views"
  val likeTopic = "likes"

  val viewCountByIdCategoryStorename = "view-count-store"
  val likeCountStorename = "like-count-store"
  val viewCountByWindowedIdAndCategoryStore = "view-count-windowed-store"
  val avgScoreStoreName = "avg-score-store"
  val highScoreStoreName = "high-score-store"
  val lowScoreStoreName = "low-score-store"

  val views = builder.stream[String, View](viewTopic)
  val likes = builder.stream[String, Like](likeTopic)

  val viewGroupedByIdAndCategory: KGroupedStream[String, View] =
    views.groupBy((_, view) => s"${view.id}-${view.view_category}")

//  val viewCountsByIdAndCategory: KTable[String, Long] =
//    viewGroupedByIdAndCategory.count()(Materialized.as(viewCountByIdCategoryStorename))

  val viewCountsByIdAndCategory: KTable[String, ViewCountWithTitle] =
    viewGroupedByIdAndCategory.aggregate(ViewCountWithTitle(0L, ""))((key, view, aggregate) =>
      ViewCountWithTitle(aggregate.count + 1, view.title)
    )(Materialized.as(viewCountByIdCategoryStorename))

  val viewWindowedCounts: KTable[Windowed[String], Long] = viewGroupedByIdAndCategory
    .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)).advanceBy(Duration.ofMinutes(1)))
    .count()(Materialized.as(viewCountByWindowedIdAndCategoryStore))

  val likesCountsById: KTable[Int, Long] = likes
    .groupBy((_, like) => like.id)
    .count()(Materialized.as(likeCountStorename))


  val averageScoreById: KTable[Int, MeanScoreforLike] = likes
    .groupBy((_, like) => like.id)
    .aggregate[MeanScoreforLike](
      initializer = MeanScoreforLike.empty
    )((_, like, aggregate) => aggregate.increment(like.score))(
      Materialized.as(avgScoreStoreName)
    )

  val highRatedMovies: KTable[Int, MeanScoreforLike] = averageScoreById
    .filter(
      (_, meanScore) => meanScore.meanScore > 4.0,
      Materialized.as[Int, MeanScoreforLike, ByteArrayKeyValueStore](highScoreStoreName)
    )

  val lowRatedMovies: KTable[Int, MeanScoreforLike] = averageScoreById
    .filter(
      (_, meanScore) => meanScore.meanScore < 2,
      Materialized.as[Int, MeanScoreforLike, ByteArrayKeyValueStore](lowScoreStoreName)
    )

  def run(): KafkaStreams = {
    val streams: KafkaStreams = new KafkaStreams(builder.build(), props)
    streams.start()


    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable() {
      override def run(): Unit = {
        streams.close()
      }
    }))
    streams
  }

  // auto loader from properties file in project
  def buildProperties: Properties = {
    val properties = new Properties()
    properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    properties.put(StreamsConfig.CLIENT_ID_CONFIG, applicationName)
    properties.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationName)
    properties.put(StreamsConfig.STATESTORE_CACHE_MAX_BYTES_CONFIG, "0")
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    properties.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, "-1")
    properties.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
    properties
  }
}
