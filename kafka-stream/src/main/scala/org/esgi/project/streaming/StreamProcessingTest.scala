package org.esgi.project.streaming

import io.github.azhur.kafka.serde.PlayJsonSupport
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.streams.scala._
import org.apache.kafka.streams.scala.kstream.{KTable, Materialized}
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.esgi.project.api.models.{Like, View}

import java.util.Properties
import java.time.Instant

object StreamProcessingTest extends PlayJsonSupport {

  import org.apache.kafka.streams.scala.ImplicitConversions._
  import org.apache.kafka.streams.scala.serialization.Serdes._

  val applicationName = s"some-application-name${Instant.now().toEpochMilli}"

  private val props: Properties = buildProperties

  // defining processing graph
  val builder: StreamsBuilder = new StreamsBuilder

  val viewTopic = "views"
  val viewCountStorename = "view-count-store"
  val likeTopic = "likes"
  val likeCountStorename = "like-count-store"

  val views = builder.stream[String, View](viewTopic)
  val likes = builder.stream[String, Like](likeTopic)

  val viewCountsById: KTable[Int, Long] = views
    .groupBy((_, view) => view.id)
    .count()(Materialized.as(viewCountStorename))

  def run(): KafkaStreams = {
    val streams: KafkaStreams = new KafkaStreams(builder.build(), props)
    streams.start()

    // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
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
