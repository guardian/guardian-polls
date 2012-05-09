package com.gu.polls.model

import com.gu.polls.util.Annotations._
import com.googlecode.objectify.annotation.Entity
import com.googlecode.objectify.ObjectifyService
import com.gu.polls.util.Ofy
import scala.collection.JavaConverters._


@Entity
case class Question(
  @Id var id: String,
  var count: Long)
{
  private def this() {this(null,0)}
}

@Entity
case class Answer(
    @Id var id: String,
    @Index var question: String,
    var count: Long
  ) {
  private def this() { this(null, null, 0) }
}

object Answer {
  def forQuestion(question: String) = Ofy.load.kind(classOf[Answer]).filter("question",question).iterable.asScala
}

