package com.gu.polls.servlets

import org.scalatra.ScalatraServlet
import com.gu.polls.util.Ofy
import com.gu.polls.model._
import cc.spray.json._

import PollJsonProtocols._
import com.weiglewilczek.slf4s.Logger
import com.gu.polls.scalatra.{ JsonSupport, TwirlSupport }

class DispatcherServlet extends ScalatraServlet with TwirlSupport with JsonSupport {
  val log = Logger(classOf[DispatcherServlet])

  override def jsonpCallbackParameterNames = Some("callback")

  get("/results/:pollId") {
    val pollId = params("pollId")
    val pollId2 = pollId.toLong
    val questions = Question.getByPollId(pollId2).map { q => q.toJson }.toList

    JsObject(Map("pollId" -> JsString(pollId), "questions" -> JsArray(questions)))
  }

  get("/") {
    val questions = Question.getByPollId(389852568)
    log.info(questions.toJson.prettyPrint)
    html.welcome.render(questions)
  }
  post("/") {
    log.info("Submitted params: " + multiParams)
    multiParams filterKeys { s => s.startsWith("q-") } foreach {
      case (qid, ansids) =>
        val pollId = params("pollId").toLong
        val questionId = qid.drop(2).toLong
        val answerIds = ansids.map { _.drop(2).toLong }
        val q = Question.getOrCreate(questionId, pollId)
        val answers = answerIds.map { Answer.getOrCreate(_, questionId) }
        answers.foreach { answer =>
          answer.count += 1
          Ofy.save.entity(answer)
        }
        q.count += 1
        log.debug("Question %s count now = %d".format(questionId, q.count))
        answers.foreach { a => log.debug("Answer %s count now = %d".format(a.id, a.count)) }
        Ofy.save.entity(q).now
    }
    redirect(params.get("returnTo").getOrElse("/"))
  }
}