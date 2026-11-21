package com.example.security

import com.example.model.AuditOutcome
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-GCM encryption engine and SHA-256 hash chain utilities.
 * Secret key stays encapsulated inside CareLoopCrypto and is never exposed in UI state or screens.
 */
class CareLoopCrypto(encodedKeyBase64: String? = null) {
  private val secureRandom = SecureRandom()
  private val secretKey: SecretKey = if (!encodedKeyBase64.isNullOrBlank()) {
    try {
      val keyBytes = decodeBase64(encodedKeyBase64)
      if (keyBytes.size == 32) {
        SecretKeySpec(keyBytes, "AES")
      } else {
        generateFreshKey()
      }
    } catch (_: Exception) {
      generateFreshKey()
    }
  } else {
    generateFreshKey()
  }

  private fun generateFreshKey(): SecretKey = KeyGenerator.getInstance("AES").apply {
    init(256, secureRandom)
  }.generateKey()

  fun exportKeyBase64(): String = encodeBase64(secretKey.encoded)

  fun encrypt(plaintext: String): String {
    val iv = ByteArray(GCM_IV_LENGTH_BYTES)
    secureRandom.nextBytes(iv)

    val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
    val ciphertext = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))

    val combined = ByteArray(iv.size + ciphertext.size)
    System.arraycopy(iv, 0, combined, 0, iv.size)
    System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
    return encodeBase64(combined)
  }

  fun decrypt(encodedCiphertext: String): String {
    if (encodedCiphertext.isBlank()) return ""
    val combined = decodeBase64(encodedCiphertext)
    require(combined.size > GCM_IV_LENGTH_BYTES) { "Invalid AES-GCM payload" }

    val iv = combined.copyOfRange(0, GCM_IV_LENGTH_BYTES)
    val ciphertext = combined.copyOfRange(GCM_IV_LENGTH_BYTES, combined.size)

    val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
    val plaintextBytes = cipher.doFinal(ciphertext)
    return String(plaintextBytes, StandardCharsets.UTF_8)
  }

  companion object {
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    val GENESIS_HASH: String = "0".repeat(64)

    fun sha256Hex(input: String): String {
      val digest = MessageDigest.getInstance("SHA-256")
      val bytes = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
      val sb = StringBuilder(bytes.size * 2)
      for (b in bytes) {
        val v = b.toInt() and 0xFF
        if (v < 16) sb.append('0')
        sb.append(v.toString(16))
      }
      return sb.toString()
    }

    fun computeAuditEntryHash(
      id: Long,
      timeLabel: String,
      who: String,
      role: String,
      action: String,
      outcome: AuditOutcome,
      previousHash: String,
    ): String {
      val canonical = "$id|$timeLabel|$who|$role|$action|${outcome.name}|$previousHash"
      return sha256Hex(canonical)
    }

    private val BASE64_CHARS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    fun encodeBase64(data: ByteArray): String {
      val out = StringBuilder(((data.size + 2) / 3) * 4)
      var i = 0
      while (i < data.size) {
        val b0 = data[i].toInt() and 0xFF
        val hasB1 = i + 1 < data.size
        val hasB2 = i + 2 < data.size
        val b1 = if (hasB1) data[i + 1].toInt() and 0xFF else 0
        val b2 = if (hasB2) data[i + 2].toInt() and 0xFF else 0

        val triple = (b0 shl 16) or (b1 shl 8) or b2
        out.append(BASE64_CHARS[(triple ushr 18) and 0x3F])
        out.append(BASE64_CHARS[(triple ushr 12) and 0x3F])
        out.append(if (hasB1) BASE64_CHARS[(triple ushr 6) and 0x3F] else '=')
        out.append(if (hasB2) BASE64_CHARS[triple and 0x3F] else '=')
        i += 3
      }
      return out.toString()
    }

    fun decodeBase64(input: String): ByteArray {
      val clean = input.trim()
      if (clean.isEmpty()) return ByteArray(0)
      require(clean.length % 4 == 0) { "Invalid Base64 length" }
      val padding = when {
        clean.endsWith("==") -> 2
        clean.endsWith("=") -> 1
        else -> 0
      }
      val outputSize = (clean.length / 4) * 3 - padding
      val out = ByteArray(outputSize)
      var outIdx = 0
      var i = 0
      while (i < clean.length) {
        val c0 = base64Index(clean[i])
        val c1 = base64Index(clean[i + 1])
        val c2 = if (clean[i + 2] == '=') 0 else base64Index(clean[i + 2])
        val c3 = if (clean[i + 3] == '=') 0 else base64Index(clean[i + 3])
        val triple = (c0 shl 18) or (c1 shl 12) or (c2 shl 6) or c3

        if (outIdx < outputSize) out[outIdx++] = ((triple ushr 16) and 0xFF).toByte()
        if (outIdx < outputSize) out[outIdx++] = ((triple ushr 8) and 0xFF).toByte()
        if (outIdx < outputSize) out[outIdx++] = (triple and 0xFF).toByte()
        i += 4
      }
      return out
    }

    private fun base64Index(ch: Char): Int = when (ch) {
      in 'A'..'Z' -> ch - 'A'
      in 'a'..'z' -> ch - 'a' + 26
      in '0'..'9' -> ch - '0' + 52
      '+' -> 62
      '/' -> 63
      else -> throw IllegalArgumentException("Invalid Base64 character: $ch")
    }
  }
}
