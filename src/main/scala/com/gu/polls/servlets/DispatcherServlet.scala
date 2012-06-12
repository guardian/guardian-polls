package com.gu.polls.servlets

import com.gu.polls.util.Ofy
import com.gu.polls.model._
import cc.spray.json._
import com.gu.polls.util.SignatureChecker

import PollJsonProtocols._
import com.weiglewilczek.slf4s.Logger
import com.gu.polls.scalatra.{ JsonSupport, TwirlSupport }
import com.googlecode.objectify.Objectify.Work
import com.googlecode.objectify.Objectify
import javax.servlet.http.Cookie
import org.scalatra._

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

class DispatcherServlet extends ScalatraServlet with TwirlSupport with JsonSupport with CookieSupport {
  val log = Logger(classOf[DispatcherServlet])

  override def jsonpCallbackParameterNames = Some("callback")

  def referringHost(request: RichRequest) = request.referrer match {
    case Some(url) => url.replace("http://", "").split('/').headOption
    case None => Some("www.guardian.co.uk")
  }

  def cookieReferrer(referrer: Option[String]) = referrer match {
    case Some(host) => "." + host.split('.').tail.mkString(".")
    case None => ".guardian.co.uk"
  }

  def updatePollCookie(cookies: SweetCookies, domain: String, pollId: String) {
    val pollCookie: List[String] = cookies.get("GU_PL") match {
      case Some(pl) => pl.split('|').toList
      case None => Nil
    }
    val localCookieOptions = CookieOptions(domain = domain, maxAge = (3600 * 24 * 365), path = "/")
    cookies.update("GU_PL", pollId :: pollCookie take 9 mkString ("|"))(localCookieOptions)

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

  get("/results/:pollId") {
    getPollAsJson(params("pollId").toLong)
  }

  get("/") {
    log.info(getPollAsJson(389852568).prettyPrint)
    html.welcome.render(Question.getByPollId(389852568))
  }

  post("/") {
    log.info("Submitted params: " + multiParams)
    val referrer = referringHost(request)

    log.info("Referrer: " + referrer)
    val signature = SignatureChecker.validateSignature(
      params.get("pollId").getOrElse(""),
      params.get("nonce").getOrElse(""),
      params.get("ts").getOrElse(""),
      params.get("signature").getOrElse(""))
    log.info("Is signature valid? " + signature)
    val pollId = params("pollId").toLong

    if (signature) {
      multiParams filterKeys { s => s.startsWith("q-") } foreach {
        case (qid, ansids) =>
          val questionId = qid.drop(2).toLong
          val answerIds = ansids.map { _.drop(2).toLong }
          Ofy.transact(new PollIncrementer(pollId, questionId, answerIds))
      }
      updatePollCookie(cookies, cookieReferrer(referrer), pollId.toString)
    }
    redirect(params.get("returnTo").getOrElse("/"))
  }
}