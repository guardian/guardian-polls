package com.gu.polls.servlets

import com.gu.polls.util.Ofy
import com.gu.polls.model._
import cc.spray.json._
import com.gu.polls.util.SignatureChecker

import PollJsonProtocols._
import com.weiglewilczek.slf4s.Logger
import com.gu.polls.scalatra.JsonSupport
import com.googlecode.objectify.Objectify.Work
import com.googlecode.objectify.{ ObjectifyService, Objectify }
import org.scalatra._
import com.google.appengine.api.NamespaceManager

class PollQuestionIncrementer(val pollId: Long, val questionId: Long) extends Work[Unit] {
  val log = Logger(classOf[PollQuestionIncrementer])
  def run(ofy: Objectify) {
    val q = Question.getOrCreate(pollId, questionId, 0, ofy)
    q.count += 1
    ofy.save.entity(q).now()
  }
}

class PollAnswerIncrementer(val questionId: Long, val answerId: Long) extends Work[Unit] {
  val log = Logger(classOf[PollAnswerIncrementer])
  def run(ofy: Objectify) {
    val answer = Answer.getOrCreate(questionId, answerId, 0, ofy)
    answer.count += 1
    ofy.save.entity(answer).now()
  }
}

class DispatcherServlet extends ScalatraServlet with JsonSupport {
  ObjectifyService.register(classOf[Question])
  ObjectifyService.register(classOf[Answer])

  val log = Logger(classOf[DispatcherServlet])

  override def jsonpCallbackParameterNames = Some("callback")

  before("/:key*") {
    log.info("Setting namespace to " + params("key"))
    NamespaceManager.set(params("key"))
  }

  def getPollAsJson(pollId: Long) = {
    JsObject(
      Map(
        "pollId" -> JsString(pollId.toString),
        "questions" -> JsArray(
          Question.getByPollId(pollId).map { q => q.toJson }.toList
        )
      )
    )
  }

  get("/:key/:pollId") {
    response.setHeader("Cache-Control", "max-age=5")
    getPollAsJson(params("pollId").toLong)
  }

  post("/:key/:pollId") {

    val signature = SignatureChecker.validateSignature(
      params.get("pollId").get,
      params.get("nonce").getOrElse("0"),
      params.get("ts").getOrElse("0"),
      params.get("signature").getOrElse(""))
    val pollId = params("pollId").toLong
    val ofy = ObjectifyService.factory.begin()

    if (signature || (params("key") == "NOSIG")) {
      log.info("Signature is valid")
      multiParams filterKeys { s => s.startsWith("q-") } foreach {
        case (qid, ansids) =>
          val questionId = qid.drop(2).toLong
          val answerIds = ansids.map { _.drop(2).toLong }
          ofy.transact(new PollQuestionIncrementer(pollId, questionId))
          answerIds foreach { answerId => ofy.transact(new PollAnswerIncrementer(questionId, answerId)) }
      }
    } else {
      log.info("Signature is invalid")
    }
    redirect(request.referrer.getOrElse("http://www.guardian.co.uk/polls"))
  }

  get("/") {
    <html>
      <head>
        <title>Guardian Polls</title>
      </head>
      <body>
        <h1>Guardian Polls</h1>
        <p>Congratulations, you've found the guardian polls system.</p>
        <p>Warning: No user servicable parts are contained within this application</p>
        <p>Return to <a href="http://www.guardian.co.uk/">The Guardian</a></p>
      </body>
    </html>
  }
}