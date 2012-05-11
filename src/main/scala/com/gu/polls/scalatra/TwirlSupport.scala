package com.gu.polls.scalatra

import org.scalatra.{ ScalatraKernel, ContentTypeInferrer }
import twirl.api.Content

trait TwirlSupport extends ScalatraKernel {
  // This is ugly due to limitations in Scala's type inference, but
  // at least the ugliness is in one place.
  override protected def contentTypeInferrer = ({
    case template: Content => template.contentType
  }: ContentTypeInferrer) orElse super.contentTypeInferrer
}