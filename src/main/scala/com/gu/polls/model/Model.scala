package com.gu.polls.model

import com.gu.polls.util.Annotations._
import com.googlecode.objectify.annotation.Entity
import com.gu.polls.util.Ofy
import scala.collection.JavaConverters._
import com.googlecode.objectify.Objectify

@Entity
case class Question(
    @Index var pollId: Long,
    @Id var id: Long,
    var count: Long) {
  private def this() { this(0, 0, 0) }
}

object Question {
  def getByPollId(pollId: Long) = Ofy.load.kind(classOf[Question]).filter("pollId", pollId).iterable.asScala
  def get(id: Long, ofy: Objectify = Ofy) = Option(ofy.load.`type`(classOf[Question]).id(id).get)
  def getOrCreate(pollId: Long, id: Long, count: Long = 0, ofy: Objectify = Ofy) = get(id, ofy) match {
    case Some(q) => q
    case None => Question(pollId, id, count)
  }
}

@Entity
case class Answer(
    @Index var question: Long,
    @Id var id: Long,
    var count: Long) {
  private def this() { this(0, 0, 0) }
}

object Answer {
  def getByQuestionId(question: Long) = Ofy.load.kind(classOf[Answer]).filter("question", question).iterable.asScala
  def get(id: Long, ofy: Objectify = Ofy) = Option(ofy.load.`type`(classOf[Answer]).id(id).get)
  def getOrCreate(questionId: Long, id: Long, count: Long = 0, ofy: Objectify = Ofy) = get(id, ofy) match {
    case Some(a) => a
    case None => Answer(questionId, id, count)
  }

}

import cc.spray.json._

object PollJsonProtocols extends DefaultJsonProtocol {
  implicit val answerFormat = jsonFormat3(Answer.apply)
  implicit object QuestionJsonFormat extends RootJsonFormat[Question] {
    def write(q: Question) = JsObject(
      "id" -> JsNumber(q.id),
      "count" -> JsNumber(q.count),
      "answers" -> JsArray(Answer.getByQuestionId(q.id) map { _.toJson } toList)
    )
    def read(value: JsValue) = throw new DeserializationException("Can't deserialise Questions")
  }
}
