package com.gu.polls.servlets

import org.scalatra.ScalatraServlet
import com.gu.polls.scalatra.TwirlSupport
import com.gu.polls.util.Ofy
import com.gu.polls.model.{Answer, Question}

class DispatcherServlet extends ScalatraServlet with TwirlSupport {

  get("/") {
    val q = Question("q-6995",7)
    Ofy.save.entity(q).now()
    Ofy.save.entities(Answer("a-12610","q-6995",2),Answer("a-12611","q-6995",4),Answer("a-12612","q-6995",1)).now()
    html.welcome.render(q, Answer.forQuestion(q.id))
  }

}