package com.gu.polls.util

import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac
import com.weiglewilczek.slf4s.Logger
import org.apache.commons.codec.binary.Base64
import java.util.Date
import com.google.appengine.api.memcache.{ Expiration, MemcacheServiceFactory }

object SignatureChecker {

  // We namespace memcache into "nonce", which saves us doing "nonce:"+key everywhere
  lazy val memcache = MemcacheServiceFactory.getMemcacheService("nonce")
  lazy val KEY = Config.get("signing.key")
  lazy val keySpec = new SecretKeySpec(KEY.getBytes(), "HmacSHA1")
  val logger = Logger("Signer")

  private def sign(key: String) = {
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(keySpec)
    Base64.encodeBase64String(mac.doFinal(key getBytes))
  }

  def validateSignature(id: String, nonce: String, ts: String, sig: String): Boolean = {
    val key = "{" + "timestamp:" + ts + ",id:" + id + ",nonce:" + nonce + "}"
    // Basic validation first
    // Is timestamp recent-ish (within 15 minutes or so)
    val ctime = new Date().getTime / 1000
    val diffTime = ctime - ts.toLong
    logger.info("Difference in times is " + diffTime)
    if (diffTime > 0 && diffTime < 900) {
      // Now check if the nonce has been used before
      logger.info("Memcache for nonce(" + nonce + ") is " + memcache.get(nonce))
      if (!memcache.contains(nonce)) {
        // Mark this nonce as having been used
        memcache.put(nonce, "1", Expiration.byDeltaSeconds(900))
        val expected = sign(key)
        logger.info("Signed: " + key + " with key: " + KEY)
        logger.info("Expected: " + expected)
        logger.info("Actual  : " + sig)
        // Compare Actual and Expected Signatures
        return expected == sig
      }
    }
    false
  }
}
