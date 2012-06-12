package com.gu.polls.servlets

import org.scalatra.ScalatraServlet
import com.gu.polls.util.Ofy
import com.gu.polls.model._
import cc.spray.json._
import com.gu.polls.util.SignatureChecker

import PollJsonProtocols._
import com.weiglewilczek.slf4s.Logger
import com.gu.polls.scalatra.{ JsonSupport, TwirlSupport }
import com.googlecode.objectify.Objectify.Work
import com.googlecode.objectify.Objectify

class PollIncrementer(val pollId: Long, val questionId: Long, val answerIds: Seq[Long]) extends Work[Unit] {
  val log = Logger(classOf[PollIncrementer])
  def run(ofy: Objectify) {
    log.info("Starting Transaction")
    val q = Question.getOrCreate(pollId, questionId)
    val answers = answerIds.map { Answer.getOrCreate(questionId, _) }
    log.info(q.toString)
    log.info((answers.map { _.toString }).mkString(","))
    answers.foreach { answer =>
      answer.count += 1
      Ofy.save.entity(answer)
    }
    q.count += 1
    Ofy.save.entity(q)
  }
}

class DispatcherServlet extends ScalatraServlet with TwirlSupport with JsonSupport {
  val log = Logger(classOf[DispatcherServlet])

  override def jsonpCallbackParameterNames = Some("callback")

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

  get("/results/:pollId") {
    getPollAsJson(params("pollId").toLong)
  }

  get("/") {
    log.info(getPollAsJson(389852568).prettyPrint)
    html.welcome.render(Question.getByPollId(389852568))
  }
  post("/") {
    log.info("Submitted params: " + multiParams)
    val signature = SignatureChecker.validateSignature(
      params.get("pollId").getOrElse(""),
      params.get("nonce").getOrElse(""),
      params.get("ts").getOrElse(""),
      params.get("signature").getOrElse(""))
    log.info("Is signature valid? " + signature)
    if (signature) {
      multiParams filterKeys { s => s.startsWith("q-") } foreach {
        case (qid, ansids) =>
          val pollId = params("pollId").toLong
          val questionId = qid.drop(2).toLong
          val answerIds = ansids.map { _.drop(2).toLong }
          Ofy.transact(new PollIncrementer(pollId, questionId, answerIds))
      }
    }
    redirect(params.get("returnTo").getOrElse("/"))
  }
}