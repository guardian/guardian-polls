package com.gu.polls.servlets

import org.scalatra.ScalatraServlet
import org.scalatra.fileupload.FileUploadSupport
import com.weiglewilczek.slf4s.Logger
import scala.io.Source
import com.gu.polls.model.{ Answer, Question }
import com.gu.polls.util.Ofy
import com.google.appengine.api.taskqueue.QueueFactory
import com.google.appengine.api.taskqueue.TaskOptions.Builder._
import cc.spray.json._
import org.apache.commons.fileupload.FileItemFactory
import org.apache.commons.fileupload.disk.DiskFileItemFactory

case class PollLine(pollId: Long, questionId: Long, questionTotal: Long, answerId: Long, answerTotal: Long)

class UploadServlet extends ScalatraServlet with FileUploadSupport {
  // Our import is a couple of hundred k, support large in-memory files
  // We know that this import is done rarely, so this should be fine.
  override lazy val fileItemFactory = new DiskFileItemFactory(512 * 1024, null)

  import DefaultJsonProtocol._
  implicit val pollLineFormat = jsonFormat5(PollLine.apply)

  val log = Logger(classOf[UploadServlet])

  post("/import") {
    val polls = Source.fromInputStream(fileParams("thefile").getInputStream)
      .getLines()
      .map { _.split(",") }
      .map { line => PollLine(line(0).toLong, line(1).toLong, line(2).toLong, line(3).toLong, line(4).toLong).toJson }
    polls.grouped(50).foreach { pollList =>
      QueueFactory.getDefaultQueue.add(
        withUrl("/data/queue")
          .param("polls", pollList.toJson.compactPrint)
      )
    }
  }

  get("/import") {
    <form method="post" enctype="multipart/form-data">
      <input type="file" name="thefile"/>
      <input type="submit"/>
    </form>
  }

  post("/queue") {
    val polls: List[PollLine] = params("polls").asJson.convertTo[List[PollLine]]
    val pollObjs = polls.flatMap { poll =>
      Answer.getOrCreate(poll.questionId, poll.answerId, poll.answerTotal) ::
        Question.getOrCreate(poll.pollId, poll.questionId, poll.questionTotal) ::
        Nil
    }
    pollObjs.foreach { p => log.info(p.toString) }
    pollObjs
      .map { Ofy.save.entity(_) }
      .map(_.now)

  }
}
