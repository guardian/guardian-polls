package com.gu.polls.servlets

import org.scalatra.ScalatraServlet
import com.google.appengine.api.NamespaceManager
import com.weiglewilczek.slf4s.Logger
import com.googlecode.objectify.ObjectifyService
import com.gu.polls.model.{ Answer, Question }

class PollFixerServlet extends ScalatraServlet {
  val log = Logger(classOf[PollFixerServlet])

  before("/:key*") {
    log.info("Setting namespace to " + params("key"))
    NamespaceManager.set(params("key"))
  }

  get("/:key/:pollId") {
    val pollId = params("pollId").toLong
    val ofy = ObjectifyService.factory.begin()
    val questions = Question.getByPollId(pollId)
    questions foreach { q =>
      log.info("Question count is: " + q.count)
      val realCount = Answer.getByQuestionId(q.id).map(_.count).foldLeft(0L) {
        (a, b) => a + b
      }
      log.info("Total should be " + realCount)
      q.count = realCount
      ofy.save().entity(q).now()
    }
  }

}
