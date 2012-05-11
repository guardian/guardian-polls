package com.gu.polls.util

import com.googlecode.objectify.annotation
import scala.annotation.target.field

object Annotations {

  type AlsoLoad = annotation.AlsoLoad @field
  type Embed = annotation.Embed @field
  type Id = annotation.Id @field
  type Ignore = annotation.Ignore @field
  type IgnoreLoad = annotation.IgnoreLoad @field
  type IgnoreSave = annotation.IgnoreSave @field
  type Index = annotation.Index @field
  type Load = annotation.Load @field
  type Mapify = annotation.Mapify @field
  type Parent = annotation.Parent @field
  type Serialize = annotation.Serialize @field
  type Translate = annotation.Translate @field
  type Unindex = annotation.Unindex @field

}