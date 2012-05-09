package com.gu.polls.servlets

import org.scalatra.ScalatraServlet
import com.gu.polls.scalatra.TwirlSupport
import com.gu.polls.util.Ofy
import com.gu.polls.model._
import cc.spray.json._

import PollJsonProtocols._

class DispatcherServlet extends ScalatraServlet with TwirlSupport {
  get("/results/:pollId") {
    val pollId = params("pollId")
    val pollId2 = pollId.toLong
    val questions = Question.getByPollId(pollId2).map { q => q.toJson}.toList

    JsObject(Map("pollId"->JsString(pollId), "questions"->JsArray(questions))).prettyPrint
  }

  get("/") {
    html.welcome.render(null, null)
  }
  post("/") {
    params filterKeys { s => s.startsWith("q-") } foreach { case (k,v) =>
      val pollId = params("pollId").toLong
      val questionId = k.drop(2).toLong
      val answerId = v.drop(2).toLong
      val q = Question.getOrCreate(questionId, pollId)
      val answer = Answer.getOrCreate(answerId, questionId)
      answer.count += 1
      q.count += 1
      Ofy.save.entities(answer,q)
    }
    redirect(params.get("returnTo").getOrElse("/"))
  }
}