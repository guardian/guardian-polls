package com.gu.polls.servlets

import org.scalatra.ScalatraServlet
import com.gu.polls.scalatra.TwirlSupport
import com.gu.polls.util.Ofy
import com.gu.polls.model._
import cc.spray.json._

import PollJsonProtocols._
import com.weiglewilczek.slf4s.Logger

class DispatcherServlet extends ScalatraServlet with TwirlSupport {
  val log = Logger(classOf[DispatcherServlet])
  get("/results/:pollId") {
    val pollId = params("pollId")
    val pollId2 = pollId.toLong
    val questions = Question.getByPollId(pollId2).map { q => q.toJson }.toList

    JsObject(Map("pollId" -> JsString(pollId), "questions" -> JsArray(questions))).prettyPrint
  }

  get("/") {
    html.welcome.render(null, null)
  }
  post("/") {
    log.info("Submitted params: " + params)
    params filterKeys { s => s.startsWith("q-") } foreach {
      case (k, v) =>
        val pollId = params("pollId").toLong
        val questionId = k.drop(2).toLong
        val answerId = v.drop(2).toLong
        val q = Question.getOrCreate(questionId, pollId)
        val answer = Answer.getOrCreate(answerId, questionId)
        answer.count += 1
        q.count += 1
        log.debug("Question %s count now = %d".format(questionId, q.count))
        log.debug("Answer %s count now = %d".format(answerId, answer.count))
        Ofy.save.entities(answer, q).now
    }
    redirect(params.get("returnTo").getOrElse("/"))
  }
}