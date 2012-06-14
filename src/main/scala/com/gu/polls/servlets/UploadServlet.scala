package com.gu.polls.servlets

import org.scalatra.ScalatraServlet
import com.weiglewilczek.slf4s.Logger
import scala.io.Source
import com.gu.polls.model.{ Answer, Question }
import com.gu.polls.util.Ofy
import com.google.appengine.api.taskqueue.QueueFactory
import com.google.appengine.api.taskqueue.TaskOptions.Builder._
import cc.spray.json._
import com.google.appengine.api.blobstore.{ BlobstoreInputStream, BlobstoreServiceFactory, BlobstoreService }
import scala.collection.JavaConverters._
import com.google.appengine.api.NamespaceManager

case class PollLine(pollId: Long, questionId: Long, questionTotal: Long, answerId: Long, answerTotal: Long)

class UploadServlet extends ScalatraServlet {
  before("/:key*") {
    log.info("Setting namespace to " + params("key"))
    NamespaceManager.set(params("key"))
  }

  import DefaultJsonProtocol._
  implicit val pollLineFormat = jsonFormat5(PollLine.apply)

  val log = Logger(classOf[UploadServlet])

  post("/:key/import") {
    val blob = BlobstoreServiceFactory.getBlobstoreService.getUploads(request).get("thefile").get(0)
    Source.fromInputStream(new BlobstoreInputStream(blob))
      .getLines()
      .map { _.split(",") }
      .map { line => PollLine(line(0).toLong, line(1).toLong, line(2).toLong, line(3).toLong, line(4).toLong).toJson }
      .grouped(25)
      .grouped(5)
      .foreach { groups =>
        QueueFactory.getDefaultQueue.add(
          groups.map { pollList =>
            withUrl("/data/" + params("key") + "/queue")
              .param("polls", pollList.toJson.compactPrint)
          }.toIterable.asJava)
      }
    BlobstoreServiceFactory.getBlobstoreService.delete(blob)
  }

  get("/:key/import") {
    val uploadUrl = BlobstoreServiceFactory.getBlobstoreService.createUploadUrl("/data/" + params("key") + "/import")
    <form method="post" enctype="multipart/form-data" action={ uploadUrl }>
      <input type="file" name="thefile"/>
      <input type="submit"/>
    </form>
  }

  post("/:key/queue") {
    val polls: List[PollLine] = params("polls").asJson.convertTo[List[PollLine]]
    polls.flatMap { poll =>
      Answer.getOrCreate(poll.questionId, poll.answerId, poll.answerTotal) ::
        Question.getOrCreate(poll.pollId, poll.questionId, poll.questionTotal) ::
        Nil
    }
      .map { Ofy.save.entity(_) }
      .map(_.now)

  }
}
