package org.esgi.project.streaming

import io.github.azhur.kafka.serde.PlayJsonSupport
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.scala.serialization.Serdes
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.test.TestRecord
import org.esgi.project.api.models.{Like, View}
import org.scalatest.funsuite.AnyFunSuite

import scala.jdk.CollectionConverters._

class StreamProcessingSpec extends AnyFunSuite with PlayJsonSupport {
  test("Topology should compute a correct word count") {
    // Given
    val messages = List(
      "hello world",
      "hello moon",
      "foobar",
      "42"
    )

    val views = List[View](
      View(1, "Kill Bill", "half"),
      View(1, "Kill Bill", "half"),
      View(1, "Kill Bill", "full"),
      View(2, "Matrix", "full")
    )

    val likes = List[Like](
      Like(1, 4.8),
      Like(1, 4.9),
      Like(2, 3.8)
    )

    val topologyTestDriver = new TopologyTestDriver(
      StreamProcessingTest.builder.build(),
      StreamProcessingTest.buildProperties
    )

    val viewTopic = topologyTestDriver
      .createInputTopic(
        StreamProcessingTest.viewTopic,
        Serdes.intSerde.serializer(),
        toSerializer[View]
      )

//    val viewCountStore: KeyValueStore[Int, Long] =
//      topologyTestDriver
//        .getKeyValueStore[Int, Long](
//          StreamProcessingTest.viewCountStorename
//        )

    val viewCountStore: KeyValueStore[String, Long] =
      topologyTestDriver
        .getKeyValueStore[String, Long](
          StreamProcessingTest.viewCountByIdCategoryStorename
        )

    val viewCountWindowedStore: KeyValueStore[String, Long] =
      topologyTestDriver
        .getKeyValueStore[String, Long](
          StreamProcessingTest.viewCountByWindowedIdAndCategoryStore
        )

    // When
    viewTopic.pipeRecordList(
      views.map(view => new TestRecord(view.id, view)).asJava
    )

    // Then
    assert(viewCountStore.get("1-half") == 2)
    assert(viewCountStore.get("1-full") == 1)
    assert(viewCountStore.get("2-half") == 0)
//    assert(viewCountWindowedStore.get("2-half") == 0)
//    assert(viewCountStore.get("hello") == 2)
//    assert(viewCountStore.get("world") == 1)
//    assert(viewCountStore.get("moon") == 1)
//    assert(viewCountStore.get("foobar") == 1)
//    assert(viewCountStore.get("42") == 1)
  }
}
