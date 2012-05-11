package com.gu.polls.model

import com.gu.polls.util.Annotations._
import com.googlecode.objectify.annotation.Entity
import com.gu.polls.util.Ofy
import scala.collection.JavaConverters._

@Entity
case class Question(
    @Index var pollId: Long,
    @Id var id: Long,
    var count: Long) {
  private def this() { this(0, 0, 0) }
}

object Question {
  def getByPollId(pollId: Long) = Ofy.load.kind(classOf[Question]).filter("pollId", pollId).iterable.asScala
  def get(id: Long) = Option(Ofy.load.kind(classOf[Question]).id(id).get)
  def getOrCreate(id: Long, pollId: Long) = get(id) match {
    case Some(q) => q
    case None => Question(pollId, id, 0)
  }
}

@Entity
case class Answer(
    @Id var id: Long,
    @Index var question: Long,
    var count: Long) {
  private def this() { this(0, 0, 0) }
}

object Answer {
  def create(answerId: Long, QuestionId: Long, count: Long) {
    Ofy.save.entity(Answer(answerId, QuestionId, count)).now
  }

  def get(id: Long) = Option(Ofy.load.kind(classOf[Answer]).id(id).get)
  def getOrCreate(id: Long, questionId: Long) = get(id) match {
    case Some(a) => a
    case None => Answer(id, questionId, 0)
  }

  def forQuestion(question: Long) = Ofy.load.kind(classOf[Answer]).filter("question", question).iterable.asScala
}

import cc.spray.json._

object PollJsonProtocols extends DefaultJsonProtocol {
  implicit val answerFormat = jsonFormat3(Answer.apply)
  implicit object QuestionJsonFormat extends RootJsonFormat[Question] {
    def write(q: Question) = JsObject(
      "id" -> JsNumber(q.id),
      "count" -> JsNumber(q.count),
      "answers" -> JsArray(Answer.forQuestion(q.id) map { _.toJson } toList)
    )
    def read(value: JsValue) = throw new DeserializationException("Can't deserialise Questions")
  }
}
