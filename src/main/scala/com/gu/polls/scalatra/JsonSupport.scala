package com.gu.polls.scalatra

import org.scalatra.{ ScalatraKernel, ContentTypeInferrer, RenderPipeline }
import cc.spray.json.JsValue

trait JsonSupport extends ScalatraKernel {
  def jsonpCallbackParameterNames: Iterable[String] = None

  override protected def contentTypeInferrer = ({
    case _: JsValue => "application/json; charset=utf-8"
  }: ContentTypeInferrer) orElse super.contentTypeInferrer

  override protected def renderPipeline = ({
    case jv: JsValue =>
      val jsonString = jv.compactPrint
      val jsonWithCallback = for {
        paramName <- jsonpCallbackParameterNames
        callback <- params.get(paramName)
      } yield "%s(%s);" format (callback, jsonString)
      jsonWithCallback.headOption.getOrElse(jsonString).getBytes("UTF-8")
  }: RenderPipeline) orElse super.renderPipeline
}
