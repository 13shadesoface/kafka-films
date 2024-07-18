package org.esgi.project.streaming

import io.github.azhur.kafka.serde.PlayJsonSupport
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.streams.scala._
import org.apache.kafka.streams.scala.kstream.{KStream, KTable, Materialized}
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.esgi.project.streaming.models.{Like, View}

import java.util.{Properties, UUID}

object StreamProcessing extends PlayJsonSupport {

  import org.apache.kafka.streams.scala.ImplicitConversions._
  import org.apache.kafka.streams.scala.serialization.Serdes._

  val applicationName = s"some-application-name"

  private val props: Properties = buildProperties

  // defining processing graph
  val builder: StreamsBuilder = new StreamsBuilder

  // topic
  val viewsTopicName: String = "views"
  val likesTopicName: String = "likes"

  val viewTopic = "views"
  val likesTopic: String = "likes"

  // Store names
  val viewsCountStoreName = "views-count-store"
  val likesCountStoreName = "likes-count-store"

  val views = builder.stream[String, String](viewTopic)
  val likes = builder.stream[String, String](likesTopic)

  val wordTopic = "words"
  val wordCountStoreName = "word-count-store"

  val words = builder.stream[String, String](wordTopic)

  val wordCounts: KTable[String, Long] = words
    .flatMapValues(textLine => textLine.toLowerCase.split("\\W+"))
    .groupBy((_, word) => word)
    .count()(Materialized.as(wordCountStoreName))

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
