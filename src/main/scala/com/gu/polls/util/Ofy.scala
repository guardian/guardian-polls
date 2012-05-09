package com.gu.polls.util

import com.google.appengine.api.datastore.{Entity,Transaction}
import com.google.appengine.api.datastore.ReadPolicy.Consistency
import com.googlecode.objectify.cmd.{Deleter,Saver,Loader,LoadType}
import com.googlecode.objectify.{ObjectifyService, TxnWork, ObjectifyFactory, Objectify}
import com.googlecode.objectify.util.cmd.LoaderWrapper
import util.DynamicVariable
import com.gu.polls.model.{Answer, Question}

/* Provides a loader that has a kind method that defers to type since type is reserved in scala */
class ScalaLoader(val loader: Loader) extends LoaderWrapper[ScalaLoader](loader) {  
  def kind[E](kind: Class[E]): LoadType[E] = loader.`type`(kind)  
}

/* Provides a threadlocal version of Objectify, lazily instantiated, one per thread
 * Kudos to Jorrit Posthuma at http://thoughtsofthree.com/2012/04/scala-objectify4/ for the code */

trait Ofy extends Objectify {
  
  def ofy: Objectify
  
  def load: ScalaLoader = new ScalaLoader(ofy.load)
  def save: Saver = ofy.save
  def delete: Deleter = ofy.delete
  def getTxn: Transaction = ofy.getTxn
  def getFactory: ObjectifyFactory = ofy.getFactory
  def consistency(policy: Consistency): Ofy = new OfyWrapper(ofy.consistency(policy))
  def deadline(value: java.lang.Double): Ofy = new OfyWrapper(ofy.deadline(value))
  def cache(value: Boolean): Ofy = new OfyWrapper(ofy.cache(value))
  def transaction(): Ofy = new OfyWrapper(ofy.transaction())
  def transact[O <: Objectify, R](work: TxnWork[O, R]): R = ofy.transact(work)
  def transact[O <: Objectify, R](limitTries: Int, work: TxnWork[O, R]): R = ofy.transact(limitTries, work)
  def clear = ofy.clear
  def setWrapper(ofy: Objectify) = ofy.setWrapper(ofy)  
}

class OfyWrapper(val base: Objectify) extends Ofy {
  
  base.setWrapper(this)
  
  override def ofy: Objectify = base
  
}

object Ofy extends Ofy {

  ObjectifyService.register(classOf[Question])
  ObjectifyService.register(classOf[Answer])


  protected val _conn = new DynamicVariable[Objectify](null)
  
  def ofy: Objectify = {
    if (_conn.value == null) {
      _conn.value = ObjectifyService.factory.begin()
      _conn.value.setWrapper(this)
    }
    
    _conn.value
  }

}