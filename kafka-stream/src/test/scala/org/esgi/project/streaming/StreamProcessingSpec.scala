package org.esgi.project.streaming

import io.github.azhur.kafka.serde.PlayJsonSupport
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.scala.serialization.Serdes
import org.apache.kafka.streams.state.{KeyValueStore, WindowStore}
import org.apache.kafka.streams.test.TestRecord
import org.esgi.project.api.models.{Like, View}
import org.esgi.project.streaming.models.MeanScoreforLike
import org.scalatest.funsuite.AnyFunSuite

import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant}
import scala.jdk.CollectionConverters._

class StreamProcessingSpec extends AnyFunSuite with PlayJsonSupport {
  test("Topology should compute a correct view and like count") {
    // Given
    val views = List[View](
      View(1, "Kill Bill", "half"),
      View(1, "Kill Bill", "half"),
      View(1, "Kill Bill", "full"),
      View(2, "Matrix", "full")
    )

    val newViews = List[View](
      View(1, "Kill Bill", "half"),
      View(1, "Kill Bill", "start_only"),
      View(2, "Matrix", "half")
    )

    val likes = List[Like](
      Like(1, 5.1),
      Like(1, 2.3),
      Like(1, 7.1),
      Like(1, 5.3),
      Like(1, 6.1),
      Like(1, 5.2),
      Like(1, 8.1),
      Like(1, 2.3), // 5.1875
      Like(2, 5.1), // 5.8
      Like(2, 7.3),
      Like(2, 5.0),
      Like(3, 1.3) //3.3
    )

// val likes = {
//      "id": 1,
//      "score": 4.8
//    }

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

    val likeTopic = topologyTestDriver
      .createInputTopic(
        StreamProcessingTest.likeTopic,
        Serdes.intSerde.serializer(),
        toSerializer[Like]
      )

    val viewCountStore: KeyValueStore[String, Long] =
      topologyTestDriver
        .getKeyValueStore[String, Long](
          StreamProcessingTest.viewCountByIdCategoryStorename
        )

    val viewCountWindowedStore: WindowStore[String, Long] =
      topologyTestDriver
        .getWindowStore[String, Long](
          StreamProcessingTest.viewCountByWindowedIdAndCategoryStore
        )

    val avgScoreStore: KeyValueStore[Int, MeanScoreforLike] =
      topologyTestDriver
        .getKeyValueStore[Int, MeanScoreforLike](
          StreamProcessingTest.avgScoreStoreName
        )

    val highRatedStore: KeyValueStore[Int, MeanScoreforLike] =
      topologyTestDriver
        .getKeyValueStore[Int, MeanScoreforLike](
          StreamProcessingTest.highScoreStoreName
        )

    val lowRatedStore: KeyValueStore[Int, MeanScoreforLike] =
      topologyTestDriver
        .getKeyValueStore[Int, MeanScoreforLike](
          StreamProcessingTest.lowScoreStoreName
        )

    val likeCountStore: KeyValueStore[Int, Long] =
      topologyTestDriver
        .getKeyValueStore[Int, Long](
          StreamProcessingTest.likeCountStorename
        )

    likeTopic.pipeRecordList(
      likes.map(like => new TestRecord(like.id, like)).asJava
    )

    // When
    val startTime = Instant.now()

    viewTopic.pipeRecordList(
      views.map(view => new TestRecord(view.id, view)).asJava
    )
    topologyTestDriver.advanceWallClockTime(Duration.ofMinutes(5))

    val windowedStartTime = startTime.plus(Duration.ofMinutes(5)).truncatedTo(ChronoUnit.MINUTES)
    val windowedEndTime = windowedStartTime.plus(Duration.ofMinutes(5))
    val recordTime = windowedStartTime.plus(Duration.ofSeconds(1))

    viewTopic.pipeRecordList(
      newViews.map(view => new TestRecord(view.id, view, recordTime)).asJava
    )
    topologyTestDriver.advanceWallClockTime(Duration.ofMinutes(5))
    val fullWindowStart = startTime.truncatedTo(ChronoUnit.MINUTES)
    val fullWindowEnd = fullWindowStart.plus(Duration.ofMinutes(10))
    
    // Then
    assert(viewCountStore.get("1-start_only") == 1)
    assert(viewCountStore.get("1-half") == 3)
    assert(viewCountStore.get("1-full") == 1)
    assert(viewCountStore.get("2-start_only") == 0)
    assert(viewCountStore.get("2-half") == 1)
    assert(viewCountStore.get("2-full") == 1)

    val windowedResults1Start =
      fetchWindowedResults(viewCountWindowedStore, "1-start_only", windowedStartTime, windowedEndTime)
    val windowedResults1Half =
      fetchWindowedResults(viewCountWindowedStore, "1-half", windowedStartTime, windowedEndTime)
    val windowedResults1Full =
      fetchWindowedResults(viewCountWindowedStore, "1-full", windowedStartTime, windowedEndTime)
    val windowedResults2Start =
      fetchWindowedResults(viewCountWindowedStore, "2-start_only", windowedStartTime, windowedEndTime)
    val windowedResults2Full =
      fetchWindowedResults(viewCountWindowedStore, "2-full", windowedStartTime, windowedEndTime)
    val windowedResults2Half =
      fetchWindowedResults(viewCountWindowedStore, "2-half", windowedStartTime, windowedEndTime)

    assert(windowedResults1Start == 1)
    assert(windowedResults1Half == 1)
    assert(windowedResults1Full == 0)
    assert(windowedResults2Start == 0)
    assert(windowedResults2Half == 1)
    assert(windowedResults2Full == 0)
    
    val highRatedMovies = highRatedStore.all().asScala.toList.sortBy(_.value.meanScore).take(3).reverse
    val lowRatedMovies = lowRatedStore.all().asScala.toList.sortBy(_.value.meanScore).take(2)

    assert(highRatedMovies.head.value.meanScore == 5.8)
    assert(lowRatedMovies.head.value.meanScore == 1.3)

    
    assert(avgScoreStore.get(1).meanScore == 5.187499999999999)
  }

  def fetchWindowedResults(store: WindowStore[String, Long], key: String, from: Instant, to: Instant): Long = {
    val iterator = store.fetch(key, from, to)
    iterator.asScala.foldLeft(0L)((agg, kv) => agg + kv.value)
  }

}
