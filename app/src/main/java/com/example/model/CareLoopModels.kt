package com.example.model

enum class AvatarGender {
  MALE,
  FEMALE,
}

enum class MedicalSpecialization(val label: String, val description: String) {
  GENERAL_MEDICINE("General Medicine", "Routine checkups, blood pressure & primary care"),
  CARDIOLOGY("Cardiology", "Heart health, blood tests & cholesterol care"),
  DERMATOLOGY("Dermatology", "Skin evaluations & follow-up treatments"),
  PEDIATRICS("Pediatrics", "Child & adolescent health visits"),
}

enum class DoctorId(
  val displayName: String,
  val shortTitle: String,
  val specialization: MedicalSpecialization,
  val defaultEmail: String,
  val defaultPhone: String,
  val gender: AvatarGender,
) {
  ALI_KHAN(
    displayName = "Dr. Ali Khan",
    shortTitle = "Dr. Ali Khan",
    specialization = MedicalSpecialization.GENERAL_MEDICINE,
    defaultEmail = "dr.ali.khan@careloop.demo",
    defaultPhone = "+92 300 1104501",
    gender = AvatarGender.MALE,
  ),
  SANA_RAUF(
    displayName = "Dr. Sana Rauf",
    shortTitle = "Dr. Sana Rauf",
    specialization = MedicalSpecialization.CARDIOLOGY,
    defaultEmail = "dr.sana.rauf@careloop.demo",
    defaultPhone = "+92 300 1104502",
    gender = AvatarGender.FEMALE,
  ),
  IMRAN_QURESHI(
    displayName = "Dr. Imran Qureshi",
    shortTitle = "Dr. Imran Qureshi",
    specialization = MedicalSpecialization.DERMATOLOGY,
    defaultEmail = "dr.imran.qureshi@careloop.demo",
    defaultPhone = "+92 300 1104503",
    gender = AvatarGender.MALE,
  ),
  HINA_FAROOQ(
    displayName = "Dr. Hina Farooq",
    shortTitle = "Dr. Hina Farooq",
    specialization = MedicalSpecialization.PEDIATRICS,
    defaultEmail = "dr.hina.farooq@careloop.demo",
    defaultPhone = "+92 300 1104504",
    gender = AvatarGender.FEMALE,
  );

  val specializationLabel: String
    get() = specialization.label

  val nameWithSpecialization: String
    get() = "$displayName · ${specialization.label}"
}

enum class ContactMethod(val label: String) {
  SMS("SMS"),
  EMAIL("Email"),
  NONE("None"),
}

enum class DoctorDecision(val label: String) {
  APPROVED("Approved"),
  OVERRIDDEN("Overridden"),
}

enum class AttendanceOutcome(val label: String) {
  ATTENDED("Attended"),
  NO_SHOW("No-show"),
}

enum class RiskTier(val label: String) {
  HIGH("High"),
  MEDIUM("Medium"),
  LOW("Low"),
}

enum class ReminderFeedbackType {
  SENT,
  FAILED,
  BLOCKED,
}

data class ReminderFeedback(
  val type: ReminderFeedbackType,
  val message: String,
)

enum class ReminderDeliveryStatus(val label: String) {
  DELIVERED("Delivered"),
  FAILED("Failed"),
  ACKNOWLEDGED("Acknowledged"),
}

enum class TimeOfDay(val label: String) {
  MORNING("Morning"),
  AFTERNOON("Afternoon"),
  EVENING("Evening"),
}

enum class AppointmentRequestStatus(val label: String) {
  PENDING("Pending"),
  BOOKED("Booked"),
  NEW_TIME_OFFERED("New time offered"),
  DECLINED("Declined"),
}

enum class NotificationCategory {
  GENERAL,
  REMINDER,
  URGENT_DECISION,
  CONFIRMED,
  CANCELLED,
  NEW_BOOKING,
  TOMORROW_UNCONFIRMED,
  OFFER_RESPONSE,
  MISSED_NO_SHOW,
  CONSENT_BLOCKED,
  DELIVERY_FAILED,
  CONSENT_OFF,
  ACCESS_DENIED,
  DOUBLE_BOOKING_BLOCKED,
  STALE_REQUEST,
  AUDIT_INTEGRITY,
}

enum class ReceptionistQueueSort(val label: String) {
  RISK("Risk"),
  TIME("Time"),
}

data class AppSettings(
  val notificationSoundEnabled: Boolean = true,
  val popupAlertsEnabled: Boolean = true,
  val receptionistQueueSort: ReceptionistQueueSort = ReceptionistQueueSort.RISK,
  val receptionistHighRiskFirst: Boolean = false,
  val doctorDefaultFollowUpDays: Int = 7,
  val doctorAiAssistantVisible: Boolean = true,
)

data class MissedAppointmentRecord(
  val id: Long,
  val dateKey: String,
  val dateLabel: String,
  val doctorName: String,
)

data class AppointmentRequest(
  val id: Long,
  val patientId: Int,
  val patientName: String,
  val doctorId: DoctorId,
  val preferredDateKey: String,
  val preferredDateLabel: String,
  val preferredTimeOfDay: TimeOfDay,
  val preferredSlotIndex: Int? = null,
  val reason: String,
  val encryptedPrivateNote: String? = null,
  val status: AppointmentRequestStatus = AppointmentRequestStatus.PENDING,
  val offeredDateKey: String? = null,
  val offeredDateLabel: String? = null,
  val offeredSlotIndex: Int? = null,
  val offeredTimeLabel: String? = null,
  val bookedDateLabel: String? = null,
  val bookedTimeLabel: String? = null,
  val declineReason: String? = null,
  val createdAtEpochMs: Long = System.currentTimeMillis(),
)

data class BookedAppointment(
  val id: Long,
  val patientId: Int,
  val doctorId: DoctorId,
  val dateKey: String,
  val slotIndex: Int,
  val timeLabel: String,
  val reason: String,
)

data class FollowUpAppointment(
  val id: Long,
  val patientId: Int,
  val doctorId: DoctorId,
  val dateKey: String,
  val slotIndex: Int,
  val timeLabel: String,
)

data class DoctorRecommendedFollowUp(
  val id: Long,
  val patientId: Int,
  val doctorId: DoctorId,
  val reason: String,
  val dueInDays: Int,
  val dueDateKey: String,
  val dueDateLabel: String,
  val booked: Boolean = false,
)

data class InboxMessage(
  val id: Long,
  val patientId: Int,
  val recipientKey: String = "patient_$patientId",
  val title: String = "Clinic Notification",
  val channelLabel: String,
  val timestampLabel: String,
  val text: String,
  val isRead: Boolean = false,
  val isReminder: Boolean = false,
  val acknowledged: Boolean = false,
  val createdAtEpochMs: Long = System.currentTimeMillis(),
  val category: NotificationCategory = NotificationCategory.GENERAL,
  val targetDoctorTab: DoctorTab? = null,
  val targetPatientId: Int? = null,
)

data class MissedFollowUpTask(
  val id: Long,
  val patientId: Int,
  val patientName: String,
  val missedDateLabel: String,
  val done: Boolean = false,
  val feedback: ReminderFeedback? = null,
)

data class DecryptedContact(
  val phone: String,
  val email: String,
)

data class AuditIntegrityResult(
  val verified: Boolean,
  val message: String,
)

data class AiReminderDraft(
  val patientId: Int,
  val text: String,
  val isEditing: Boolean = false,
  val usedFallback: Boolean = false,
  val statusNotice: String? = null,
  val wasEdited: Boolean = false,
  val editLogged: Boolean = false,
)

data class DoctorChatMessage(
  val id: Long,
  val isUser: Boolean,
  val text: String,
  val timestampLabel: String,
  val isError: Boolean = false,
  val failureReason: String? = null,
  val sourceLabel: String? = null,
)

data class DoctorDailySummary(
  val doctorId: DoctorId,
  val text: String,
  val isLoading: Boolean = false,
  val usedFallback: Boolean = false,
  val statusNotice: String? = null,
)

data class UserProfile(
  val accountKey: String,
  val name: String,
  val phone: String,
  val email: String,
  val gender: AvatarGender,
  val photoDataUrl: String? = null,
  val doctorId: DoctorId? = null,
  val patientId: Int? = null,
)

data class Patient(
  val id: Int,
  val name: String,
  val age: Int,
  val gender: AvatarGender,
  val assignedDoctor: DoctorId,
  val appointmentHoursFromNow: Int,
  val appointmentDateKey: String,
  val appointmentSlotIndex: Int,
  val appointmentTimeLabel: String,
  val pastMissedAppointments: Int,
  val missedHistory: List<MissedAppointmentRecord> = emptyList(),
  val confirmed: Boolean,
  val cancelled: Boolean = false,
  val rescheduled: Boolean = false,
  val preferredContactMethod: ContactMethod,
  val smsConsent: Boolean,
  val emailConsent: Boolean,
  val encryptedPhone: String,
  val encryptedEmail: String,
  val encryptedMedicalNote: String? = null,
  val hasPhoneContact: Boolean = true,
  val hasEmailContact: Boolean = true,
  val isUrgent: Boolean = false,
  val doctorDecision: DoctorDecision? = null,
  val attendanceOutcome: AttendanceOutcome? = null,
  val attendanceRecordedAt: String? = null,
  val accountEmailOverride: String? = null,
) {
  val demoEmail: String
    get() = accountEmailOverride ?: (name.lowercase().trim().replace(" ", ".") + "@careloop.demo")

  val firstName: String
    get() = name.substringBefore(" ")

  fun isWithin48Hours(): Boolean = appointmentHoursFromNow <= 48

  fun hasConsentForPreferredMethod(): Boolean = when (preferredContactMethod) {
    ContactMethod.SMS -> smsConsent
    ContactMethod.EMAIL -> emailConsent
    ContactMethod.NONE -> false
  }

  fun hasContactForPreferredMethod(): Boolean = when (preferredContactMethod) {
    ContactMethod.SMS -> hasPhoneContact
    ContactMethod.EMAIL -> hasEmailContact
    ContactMethod.NONE -> false
  }

  fun riskScore(): Int {
    var score = pastMissedAppointments * 30
    if (!confirmed) score += 20
    if (isUrgent) score += 25
    if (isWithin48Hours()) score += 10
    return score
  }

  fun riskTier(): RiskTier {
    val score = riskScore()
    return when {
      score >= 60 -> RiskTier.HIGH
      score >= 30 -> RiskTier.MEDIUM
      else -> RiskTier.LOW
    }
  }

  /**
   * One-line risk explanation for Receptionist. Never exposes medical notes.
   */
  fun riskReasonSummary(): String {
    val parts = mutableListOf<String>()
    if (pastMissedAppointments > 0) {
      parts += "missed $pastMissedAppointments before"
    }
    if (cancelled) {
      parts += "cancelled"
    } else if (!confirmed) {
      parts += "not confirmed"
    }
    if (isUrgent) {
      parts += "urgent follow-up"
    }
    if (isWithin48Hours()) {
      parts += "within 48 hours"
    }
    if (parts.isEmpty()) {
      return "Confirmed, low risk."
    }
    val joined = parts.joinToString(", ")
    return joined.replaceFirstChar { it.uppercase() } + "."
  }
}

enum class UserRole(val roleLabel: String) {
  RECEPTIONIST("Receptionist"),
  DOCTOR("Doctor"),
  PATIENT("Patient"),
  ADMIN("Admin"),
}

data class LoggedInUser(
  val accountKey: String,
  val email: String,
  val displayName: String,
  val roleLabel: String,
  val role: UserRole,
  val doctorId: DoctorId? = null,
  val patientId: Int? = null,
) {
  val notificationRecipientKey: String
    get() = when (role) {
      UserRole.PATIENT -> "patient_${patientId ?: 0}"
      UserRole.DOCTOR -> "doctor_${doctorId?.name ?: ""}"
      UserRole.RECEPTIONIST -> "receptionist"
      UserRole.ADMIN -> "admin"
    }
}

data class DemoAccountItem(
  val accountKey: String,
  val email: String,
  val roleSummary: String,
)

enum class AuditOutcome(val label: String) {
  ALLOWED("allowed"),
  DENIED("denied"),
}

data class AuditLogEntry(
  val id: Long,
  val timeLabel: String,
  val who: String,
  val role: String,
  val action: String,
  val outcome: AuditOutcome,
  val previousHash: String,
  val hash: String,
)

enum class PatientTab(val label: String) {
  HOME("Home"),
  APPOINTMENTS("Appointments"),
  FOLLOW_UPS("Follow-ups"),
}

enum class ReceptionistTab(val label: String) {
  QUEUE("Queue"),
  REQUESTS("Requests"),
  FOLLOW_UPS("Follow-ups"),
}

enum class DoctorTab(val label: String) {
  TODAY("Today"),
  NEEDS_DECISION("Needs decision"),
  PATIENTS("Patients"),
}

enum class AdminTab(val label: String) {
  OVERVIEW("Overview"),
  REPORTS("Reports"),
  AUDIT_LOG("Audit Log"),
  SECURITY("Security"),
  DOCTORS("Doctors"),
  SETTINGS("Settings"),
}

sealed interface CareLoopScreen {
  data object Login : CareLoopScreen
  data class Receptionist(val tab: ReceptionistTab = ReceptionistTab.QUEUE) : CareLoopScreen
  data class Doctor(val doctorId: DoctorId, val tab: DoctorTab = DoctorTab.TODAY) : CareLoopScreen
  data class PatientPortal(val patientId: Int, val tab: PatientTab = PatientTab.HOME) : CareLoopScreen
  data class Admin(val tab: AdminTab = AdminTab.OVERVIEW) : CareLoopScreen
  data class Profile(val returnScreen: CareLoopScreen) : CareLoopScreen
  data class Settings(val returnScreen: CareLoopScreen) : CareLoopScreen
}
