package com.example.ai

import android.util.Log
import com.example.model.BookedAppointment
import com.example.model.ClinicSchedule
import com.example.model.DoctorId
import com.example.model.FollowUpAppointment
import com.example.model.Patient
import com.example.model.RiskTier
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

private const val TAG = "CareLoopAiGateway"

/**
 * Privacy-minimized payload for AI appointment reminder drafting.
 * Strictly excludes medical notes, age, risk score, and contact addresses.
 */
data class ReminderDraftRequest(
  val firstName: String,
  val appointmentDateTime: String,
  val doctorName: String,
  val preferredChannel: String,
)

/**
 * Privacy-minimized payload for Doctor daily high-risk summary and AI assistant.
 * Strictly excludes medical notes.
 */
data class HighRiskSummaryPatient(
  val firstName: String,
  val appointmentDateTime: String,
  val riskReason: String,
)

data class DoctorAssistantContext(
  val doctorName: String,
  val specialization: String,
  val totalAppointments: Int,
  val urgentCount: Int,
  val totalMissedCount: Int,
  val patients: List<HighRiskSummaryPatient>,
)

data class AiTextResponse(
  val text: String,
  val usedFallback: Boolean,
  val errorReason: String? = null,
)

interface CareLoopAiGateway {
  suspend fun draftReminder(request: ReminderDraftRequest): AiTextResponse

  suspend fun summarizeDoctorTopRisk(
    doctorName: String,
    topPatients: List<HighRiskSummaryPatient>,
  ): AiTextResponse

  suspend fun askDoctorAssistant(
    context: DoctorAssistantContext,
    question: String,
  ): AiTextResponse
}

class FirebaseCareLoopAiGateway : CareLoopAiGateway {
  private val model by lazy {
    Firebase.ai.generativeModel("gemini-3.8-flash")
  }

  override suspend fun draftReminder(request: ReminderDraftRequest): AiTextResponse {
    val prompt = buildString {
      appendLine("Write a warm, short appointment reminder message for a clinic patient.")
      appendLine("Rules:")
      appendLine("- Maximum 3 sentences.")
      appendLine("- Plain, friendly language.")
      appendLine("- Include NO medical information, diagnoses, or clinical details.")
      appendLine("- Ask the patient to confirm their appointment or request another time.")
      appendLine("- Return only the message text, with no subject line, quotes, or markdown.")
      appendLine()
      appendLine("Patient first name: ${request.firstName}")
      appendLine("Appointment date and time: ${request.appointmentDateTime}")
      appendLine("Doctor: ${request.doctorName}")
      appendLine("Delivery channel: ${request.preferredChannel}")
    }

    return try {
      val response = withTimeout(AI_TIMEOUT_MS) {
        model.generateContent(prompt)
      }
      val generated = response.text?.trim().orEmpty()
      if (generated.isNotEmpty()) {
        AiTextResponse(text = generated, usedFallback = false)
      } else {
        Log.e(TAG, "Reminder drafting returned empty response.")
        AiTextResponse(
          text = buildTemplateReminder(request),
          usedFallback = true,
          errorReason = REASON_NO_RESPONSE,
        )
      }
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Reminder drafting timed out after ${AI_TIMEOUT_MS}ms", e)
      AiTextResponse(
        text = buildTemplateReminder(request),
        usedFallback = true,
        errorReason = REASON_NO_RESPONSE,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Reminder drafting failed", e)
      AiTextResponse(
        text = buildTemplateReminder(request),
        usedFallback = true,
        errorReason = classifyFailureReason(e),
      )
    }
  }

  override suspend fun summarizeDoctorTopRisk(
    doctorName: String,
    topPatients: List<HighRiskSummaryPatient>,
  ): AiTextResponse {
    if (topPatients.isEmpty()) {
      return AiTextResponse(
        text = "$doctorName has no scheduled appointments to summarize today.",
        usedFallback = false,
      )
    }

    val prompt = buildString {
      appendLine("Write a concise daily briefing for $doctorName about their top ${topPatients.size} highest-risk patients.")
      appendLine("Rules:")
      appendLine("- Write exactly 3 short sentences.")
      appendLine("- Use only first names, appointment times, and the risk reasons provided below.")
      appendLine("- Never include or invent medical notes, symptoms, or diagnoses.")
      appendLine("- Plain text only, no bullet points or markdown.")
      appendLine()
      topPatients.forEachIndexed { index, patient ->
        appendLine("${index + 1}. First name: ${patient.firstName}; Appointment: ${patient.appointmentDateTime}; Risk reason: ${patient.riskReason}")
      }
    }

    return try {
      val response = withTimeout(AI_TIMEOUT_MS) {
        model.generateContent(prompt)
      }
      val generated = response.text?.trim().orEmpty()
      if (generated.isNotEmpty()) {
        AiTextResponse(text = generated, usedFallback = false)
      } else {
        Log.e(TAG, "Doctor summary generation returned empty response.")
        AiTextResponse(
          text = buildFallbackDoctorSummary(doctorName, topPatients),
          usedFallback = true,
          errorReason = REASON_NO_RESPONSE,
        )
      }
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Doctor summary generation timed out after ${AI_TIMEOUT_MS}ms", e)
      AiTextResponse(
        text = buildFallbackDoctorSummary(doctorName, topPatients),
        usedFallback = true,
        errorReason = REASON_NO_RESPONSE,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Doctor summary generation failed", e)
      AiTextResponse(
        text = buildFallbackDoctorSummary(doctorName, topPatients),
        usedFallback = true,
        errorReason = classifyFailureReason(e),
      )
    }
  }

  override suspend fun askDoctorAssistant(
    context: DoctorAssistantContext,
    question: String,
  ): AiTextResponse {
    val prompt = buildString {
      appendLine("You are CareLoop AI Assistant helping ${context.doctorName} (${context.specialization}) review their clinic schedule, no-show risk, and missed appointments.")
      appendLine("Rules:")
      appendLine("- Answer clearly in 2 to 4 short sentences.")
      appendLine("- Do NOT give medical advice, diagnoses, or clinical recommendations.")
      appendLine("- Use ONLY the first names, times, counts, and risk reasons below.")
      appendLine("- Plain text only, no markdown.")
      appendLine()
      appendLine("Summary counts:")
      appendLine("- Scheduled patients: ${context.totalAppointments}")
      appendLine("- Urgent decisions pending: ${context.urgentCount}")
      appendLine("- Total past missed appointments across patients: ${context.totalMissedCount}")
      appendLine()
      appendLine("Patients (first name, time, risk reason):")
      context.patients.forEachIndexed { index, p ->
        appendLine("${index + 1}. ${p.firstName} | ${p.appointmentDateTime} | ${p.riskReason}")
      }
      appendLine()
      appendLine("Doctor's question: $question")
    }

    return try {
      val response = withTimeout(AI_TIMEOUT_MS) {
        model.generateContent(prompt)
      }
      val generated = response.text?.trim().orEmpty()
      if (generated.isNotEmpty()) {
        AiTextResponse(text = generated, usedFallback = false)
      } else {
        Log.e(TAG, "Doctor AI assistant returned empty response.")
        AiTextResponse(
          text = REASON_NO_RESPONSE,
          usedFallback = true,
          errorReason = REASON_NO_RESPONSE,
        )
      }
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Doctor AI assistant timed out after ${AI_TIMEOUT_MS}ms", e)
      AiTextResponse(
        text = REASON_NO_RESPONSE,
        usedFallback = true,
        errorReason = REASON_NO_RESPONSE,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Doctor AI assistant failed", e)
      val reason = classifyFailureReason(e)
      AiTextResponse(
        text = reason,
        usedFallback = true,
        errorReason = reason,
      )
    }
  }

  companion object {
    private const val AI_TIMEOUT_MS = 60_000L
    const val REASON_NO_INTERNET = "No internet connection"
    const val REASON_NOT_CONFIGURED = "AI is not configured"
    const val REASON_NO_RESPONSE = "AI service did not respond"

    fun classifyFailureReason(throwable: Throwable): String {
      var cur: Throwable? = throwable
      while (cur != null) {
        if (cur is UnknownHostException || cur is ConnectException || cur is SocketException) {
          return REASON_NO_INTERNET
        }
        if (cur is TimeoutCancellationException || cur is SocketTimeoutException) {
          return REASON_NO_RESPONSE
        }
        val msg = cur.message.orEmpty().lowercase(Locale.US)
        if (
          msg.contains("unable to resolve host") ||
          msg.contains("failed to connect") ||
          msg.contains("network") ||
          msg.contains("offline") ||
          msg.contains("no address associated") ||
          msg.contains("internet")
        ) {
          return REASON_NO_INTERNET
        }
        if (
          msg.contains("firebaseapp") ||
          msg.contains("app check") ||
          msg.contains("appcheck") ||
          msg.contains("not configured") ||
          msg.contains("api key") ||
          msg.contains("apikey") ||
          msg.contains("403") ||
          msg.contains("permission_denied") ||
          msg.contains("unauthenticated") ||
          msg.contains("default firebaseapp is not initialized")
        ) {
          return REASON_NOT_CONFIGURED
        }
        cur = cur.cause
      }
      return REASON_NO_RESPONSE
    }

    fun buildTemplateReminder(request: ReminderDraftRequest): String {
      return "Hello ${request.firstName}, this is a reminder for your upcoming appointment with ${request.doctorName} (${request.appointmentDateTime}). Please confirm your visit or request another time in CareLoop if you need to reschedule."
    }

    fun buildFallbackDoctorSummary(
      doctorName: String,
      topPatients: List<HighRiskSummaryPatient>,
    ): String {
      if (topPatients.isEmpty()) {
        return "$doctorName has no scheduled appointments to review today."
      }
      val sentences = topPatients.take(3).map { p ->
        val cleanReason = p.riskReason.trimEnd('.').replaceFirstChar { it.lowercase() }
        "${p.firstName} (${p.appointmentDateTime}) has elevated risk due to $cleanReason."
      }
      return sentences.joinToString(" ")
    }
  }
}

/**
 * Local backup assistant used when Gemini fails.
 * Answers using first names, times, and counts only, never medical notes.
 */
object ClinicBackupAssistant {
  fun answer(
    doctorId: DoctorId,
    doctorPatients: List<Patient>,
    bookedAppointments: List<BookedAppointment>,
    followUpAppointments: List<FollowUpAppointment>,
    question: String,
  ): String {
    val q = question.trim().lowercase(Locale.US)
    val todayKey = ClinicSchedule.todayLocalDateKey()

    return when {
      q.contains("high-risk") || q.contains("high risk") || q.contains("risk") -> {
        val highRisk = doctorPatients
          .filter { !it.cancelled && it.riskTier() == RiskTier.HIGH }
          .sortedByDescending { it.riskScore() }
        if (highRisk.isEmpty()) {
          "You have 0 high-risk patients right now."
        } else {
          val list = highRisk.joinToString(", ") { "${it.firstName} (${it.appointmentTimeLabel})" }
          "${highRisk.size} high-risk patient(s): $list."
        }
      }

      q.contains("missed") || q.contains("no-show") || q.contains("no show") || q.contains("absent") -> {
        val missedPatients = doctorPatients
          .filter { it.pastMissedAppointments > 0 }
          .sortedByDescending { it.pastMissedAppointments }
        val totalMissed = doctorPatients.sumOf { it.pastMissedAppointments }
        if (totalMissed == 0) {
          "You have 0 missed appointments on record."
        } else {
          val list = missedPatients.joinToString(", ") { "${it.firstName} (${it.pastMissedAppointments})" }
          "$totalMissed missed appointment(s) across ${missedPatients.size} patient(s): $list."
        }
      }

      q.contains("urgent") || q.contains("decision") || q.contains("priority") -> {
        val urgent = doctorPatients.filter { it.isUrgent && !it.cancelled }
        val pendingCount = urgent.count { it.doctorDecision == null }
        if (urgent.isEmpty()) {
          "You have 0 urgent patients right now."
        } else {
          val list = urgent.joinToString(", ") { "${it.firstName} (${it.appointmentTimeLabel})" }
          "${urgent.size} urgent patient(s), $pendingCount needing decision: $list."
        }
      }

      q.contains("unconfirmed") || q.contains("not confirmed") || q.contains("confirm") -> {
        val unconfirmed = doctorPatients.filter { !it.confirmed && !it.cancelled && it.attendanceOutcome == null }
        if (unconfirmed.isEmpty()) {
          "You have 0 unconfirmed patients right now."
        } else {
          val list = unconfirmed.joinToString(", ") { "${it.firstName} (${it.appointmentTimeLabel})" }
          "${unconfirmed.size} unconfirmed patient(s): $list."
        }
      }

      q.contains("today") || q.contains("appointment") || q.contains("schedule") || q.contains("visit") -> {
        val todayPatients = doctorPatients
          .filter { !it.cancelled && it.appointmentDateKey == todayKey }
          .sortedBy { it.appointmentSlotIndex }
          .map { "${it.firstName} (${ClinicSchedule.slotLabel(it.appointmentSlotIndex)})" }
        val todayBooked = bookedAppointments
          .filter { it.doctorId == doctorId && it.dateKey == todayKey }
          .mapNotNull { b ->
            doctorPatients.find { it.id == b.patientId }?.let {
              "${it.firstName} (${ClinicSchedule.slotLabel(b.slotIndex)})"
            }
          }
        val todayFollowUps = followUpAppointments
          .filter { it.doctorId == doctorId && it.dateKey == todayKey }
          .mapNotNull { f ->
            doctorPatients.find { it.id == f.patientId }?.let {
              "${it.firstName} (${ClinicSchedule.slotLabel(f.slotIndex)})"
            }
          }
        val allToday = todayPatients + todayBooked + todayFollowUps
        if (allToday.isEmpty()) {
          val activeCount = doctorPatients.count { !it.cancelled }
          "You have 0 appointments today ($activeCount upcoming overall)."
        } else {
          "Today you have ${allToday.size} appointment(s): ${allToday.joinToString(", ")}."
        }
      }

      else -> {
        "Hello ${doctorId.displayName}! I can help with: today's appointments, who is high-risk, missed appointments, urgent patients, and who is unconfirmed."
      }
    }
  }
}
