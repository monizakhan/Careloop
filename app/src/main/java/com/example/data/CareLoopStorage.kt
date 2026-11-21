package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AppSettings
import com.example.model.AppointmentRequest
import com.example.model.AppointmentRequestStatus
import com.example.model.AttendanceOutcome
import com.example.model.AuditLogEntry
import com.example.model.AuditOutcome
import com.example.model.AvatarGender
import com.example.model.BookedAppointment
import com.example.model.ContactMethod
import com.example.model.DoctorDecision
import com.example.model.DoctorId
import com.example.model.DoctorRecommendedFollowUp
import com.example.model.DoctorTab
import com.example.model.FollowUpAppointment
import com.example.model.InboxMessage
import com.example.model.MissedAppointmentRecord
import com.example.model.MissedFollowUpTask
import com.example.model.NotificationCategory
import com.example.model.Patient
import com.example.model.ReceptionistQueueSort
import com.example.model.ReminderDeliveryStatus
import com.example.model.ReminderFeedback
import com.example.model.ReminderFeedbackType
import com.example.model.TimeOfDay
import com.example.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject

data class PersistedCareLoopSnapshot(
  val encryptionKeyBase64: String,
  val patients: List<Patient>,
  val userProfiles: Map<String, UserProfile>,
  val inboxMessages: List<InboxMessage>,
  val appointmentRequests: List<AppointmentRequest>,
  val bookedAppointments: List<BookedAppointment>,
  val followUpAppointments: List<FollowUpAppointment>,
  val doctorRecommendedFollowUps: List<DoctorRecommendedFollowUp>,
  val missedFollowUpTasks: List<MissedFollowUpTask>,
  val reminderStatusByPatientId: Map<Int, ReminderDeliveryStatus>,
  val reminderFeedbackByPatientId: Map<Int, ReminderFeedback>,
  val auditLogs: List<AuditLogEntry>,
  val appSettings: AppSettings = AppSettings(),
  val nextAuditId: Long,
  val nextMessageId: Long,
  val nextRequestId: Long,
  val nextBookedId: Long,
  val nextFollowUpId: Long,
  val nextDoctorFollowUpId: Long,
  val nextMissedTaskId: Long,
  val revision: Long,
)

interface CareLoopStorage {
  fun load(): PersistedCareLoopSnapshot?
  fun save(snapshot: PersistedCareLoopSnapshot)
  fun clear()
  fun registerExternalChangeListener(onChanged: () -> Unit): () -> Unit
}

object InMemoryCareLoopStorage : CareLoopStorage {
  override fun load(): PersistedCareLoopSnapshot? = null
  override fun save(snapshot: PersistedCareLoopSnapshot) = Unit
  override fun clear() = Unit
  override fun registerExternalChangeListener(onChanged: () -> Unit): () -> Unit = {}
}

class SharedPrefsCareLoopStorage(context: Context) : CareLoopStorage {
  private val prefs: SharedPreferences = context.applicationContext
    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  @Volatile
  private var lastSavedRevision: Long = -1L

  override fun load(): PersistedCareLoopSnapshot? {
    return try {
      val raw = prefs.getString(KEY_STATE_JSON, null) ?: return null
      val root = JSONObject(raw)
      parseSnapshot(root)
    } catch (_: Exception) {
      null
    }
  }

  override fun save(snapshot: PersistedCareLoopSnapshot) {
    try {
      lastSavedRevision = snapshot.revision
      val json = serializeSnapshot(snapshot).toString()
      prefs.edit().putString(KEY_STATE_JSON, json).apply()
    } catch (_: Exception) {
      // Storage failures must never crash the app
    }
  }

  override fun clear() {
    try {
      lastSavedRevision = -1L
      prefs.edit().remove(KEY_STATE_JSON).apply()
    } catch (_: Exception) {
      // Ignore storage failures safely
    }
  }

  override fun registerExternalChangeListener(onChanged: () -> Unit): () -> Unit {
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      if (key == KEY_STATE_JSON) {
        val loaded = load()
        if (loaded != null && loaded.revision != lastSavedRevision) {
          onChanged()
        }
      }
    }
    try {
      prefs.registerOnSharedPreferenceChangeListener(listener)
    } catch (_: Exception) {
      // Ignore listener issues safely
    }
    return {
      try {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
      } catch (_: Exception) {
      }
    }
  }

  private fun serializeSnapshot(s: PersistedCareLoopSnapshot): JSONObject {
    return JSONObject().apply {
      put("encryptionKeyBase64", s.encryptionKeyBase64)
      put("nextAuditId", s.nextAuditId)
      put("nextMessageId", s.nextMessageId)
      put("nextRequestId", s.nextRequestId)
      put("nextBookedId", s.nextBookedId)
      put("nextFollowUpId", s.nextFollowUpId)
      put("nextDoctorFollowUpId", s.nextDoctorFollowUpId)
      put("nextMissedTaskId", s.nextMissedTaskId)
      put("revision", s.revision)

      put("appSettings", JSONObject().apply {
        put("notificationSoundEnabled", s.appSettings.notificationSoundEnabled)
        put("popupAlertsEnabled", s.appSettings.popupAlertsEnabled)
        put("receptionistQueueSort", s.appSettings.receptionistQueueSort.name)
        put("receptionistHighRiskFirst", s.appSettings.receptionistHighRiskFirst)
        put("doctorDefaultFollowUpDays", s.appSettings.doctorDefaultFollowUpDays)
        put("doctorAiAssistantVisible", s.appSettings.doctorAiAssistantVisible)
      })

      put("patients", JSONArray().apply {
        s.patients.forEach { p ->
          put(JSONObject().apply {
            put("id", p.id)
            put("name", p.name)
            put("age", p.age)
            put("gender", p.gender.name)
            put("assignedDoctor", p.assignedDoctor.name)
            put("appointmentHoursFromNow", p.appointmentHoursFromNow)
            put("appointmentDateKey", p.appointmentDateKey)
            put("appointmentSlotIndex", p.appointmentSlotIndex)
            put("appointmentTimeLabel", p.appointmentTimeLabel)
            put("pastMissedAppointments", p.pastMissedAppointments)
            put("missedHistory", JSONArray().apply {
              p.missedHistory.forEach { rec ->
                put(JSONObject().apply {
                  put("id", rec.id)
                  put("dateKey", rec.dateKey)
                  put("dateLabel", rec.dateLabel)
                  put("doctorName", rec.doctorName)
                })
              }
            })
            put("confirmed", p.confirmed)
            put("cancelled", p.cancelled)
            put("rescheduled", p.rescheduled)
            put("preferredContactMethod", p.preferredContactMethod.name)
            put("smsConsent", p.smsConsent)
            put("emailConsent", p.emailConsent)
            put("encryptedPhone", p.encryptedPhone)
            put("encryptedEmail", p.encryptedEmail)
            put("encryptedMedicalNote", p.encryptedMedicalNote ?: JSONObject.NULL)
            put("hasPhoneContact", p.hasPhoneContact)
            put("hasEmailContact", p.hasEmailContact)
            put("isUrgent", p.isUrgent)
            put("doctorDecision", p.doctorDecision?.name ?: JSONObject.NULL)
            put("attendanceOutcome", p.attendanceOutcome?.name ?: JSONObject.NULL)
            put("attendanceRecordedAt", p.attendanceRecordedAt ?: JSONObject.NULL)
            put("accountEmailOverride", p.accountEmailOverride ?: JSONObject.NULL)
          })
        }
      })

      put("userProfiles", JSONArray().apply {
        s.userProfiles.values.forEach { u ->
          put(JSONObject().apply {
            put("accountKey", u.accountKey)
            put("name", u.name)
            put("phone", u.phone)
            put("email", u.email)
            put("gender", u.gender.name)
            put("photoDataUrl", u.photoDataUrl ?: JSONObject.NULL)
            put("doctorId", u.doctorId?.name ?: JSONObject.NULL)
            put("patientId", u.patientId ?: JSONObject.NULL)
          })
        }
      })

      put("inboxMessages", JSONArray().apply {
        s.inboxMessages.forEach { m ->
          put(JSONObject().apply {
            put("id", m.id)
            put("patientId", m.patientId)
            put("recipientKey", m.recipientKey)
            put("title", m.title)
            put("channelLabel", m.channelLabel)
            put("timestampLabel", m.timestampLabel)
            put("text", m.text)
            put("isRead", m.isRead)
            put("isReminder", m.isReminder)
            put("acknowledged", m.acknowledged)
            put("createdAtEpochMs", m.createdAtEpochMs)
            put("category", m.category.name)
            put("targetDoctorTab", m.targetDoctorTab?.name ?: JSONObject.NULL)
            put("targetPatientId", m.targetPatientId ?: JSONObject.NULL)
          })
        }
      })

      put("appointmentRequests", JSONArray().apply {
        s.appointmentRequests.forEach { r ->
          put(JSONObject().apply {
            put("id", r.id)
            put("patientId", r.patientId)
            put("patientName", r.patientName)
            put("doctorId", r.doctorId.name)
            put("preferredDateKey", r.preferredDateKey)
            put("preferredDateLabel", r.preferredDateLabel)
            put("preferredTimeOfDay", r.preferredTimeOfDay.name)
            put("preferredSlotIndex", r.preferredSlotIndex ?: JSONObject.NULL)
            put("reason", r.reason)
            put("encryptedPrivateNote", r.encryptedPrivateNote ?: JSONObject.NULL)
            put("status", r.status.name)
            put("offeredDateKey", r.offeredDateKey ?: JSONObject.NULL)
            put("offeredDateLabel", r.offeredDateLabel ?: JSONObject.NULL)
            put("offeredSlotIndex", r.offeredSlotIndex ?: JSONObject.NULL)
            put("offeredTimeLabel", r.offeredTimeLabel ?: JSONObject.NULL)
            put("bookedDateLabel", r.bookedDateLabel ?: JSONObject.NULL)
            put("bookedTimeLabel", r.bookedTimeLabel ?: JSONObject.NULL)
            put("declineReason", r.declineReason ?: JSONObject.NULL)
            put("createdAtEpochMs", r.createdAtEpochMs)
          })
        }
      })

      put("bookedAppointments", JSONArray().apply {
        s.bookedAppointments.forEach { b ->
          put(JSONObject().apply {
            put("id", b.id)
            put("patientId", b.patientId)
            put("doctorId", b.doctorId.name)
            put("dateKey", b.dateKey)
            put("slotIndex", b.slotIndex)
            put("timeLabel", b.timeLabel)
            put("reason", b.reason)
          })
        }
      })

      put("followUpAppointments", JSONArray().apply {
        s.followUpAppointments.forEach { f ->
          put(JSONObject().apply {
            put("id", f.id)
            put("patientId", f.patientId)
            put("doctorId", f.doctorId.name)
            put("dateKey", f.dateKey)
            put("slotIndex", f.slotIndex)
            put("timeLabel", f.timeLabel)
          })
        }
      })

      put("doctorRecommendedFollowUps", JSONArray().apply {
        s.doctorRecommendedFollowUps.forEach { df ->
          put(JSONObject().apply {
            put("id", df.id)
            put("patientId", df.patientId)
            put("doctorId", df.doctorId.name)
            put("reason", df.reason)
            put("dueInDays", df.dueInDays)
            put("dueDateKey", df.dueDateKey)
            put("dueDateLabel", df.dueDateLabel)
            put("booked", df.booked)
          })
        }
      })

      put("missedFollowUpTasks", JSONArray().apply {
        s.missedFollowUpTasks.forEach { t ->
          put(JSONObject().apply {
            put("id", t.id)
            put("patientId", t.patientId)
            put("patientName", t.patientName)
            put("missedDateLabel", t.missedDateLabel)
            put("done", t.done)
            if (t.feedback != null) {
              put("feedbackType", t.feedback.type.name)
              put("feedbackMessage", t.feedback.message)
            } else {
              put("feedbackType", JSONObject.NULL)
              put("feedbackMessage", JSONObject.NULL)
            }
          })
        }
      })

      put("reminderStatusByPatientId", JSONObject().apply {
        s.reminderStatusByPatientId.forEach { (k, v) -> put(k.toString(), v.name) }
      })

      put("reminderFeedbackByPatientId", JSONObject().apply {
        s.reminderFeedbackByPatientId.forEach { (k, v) ->
          put(k.toString(), JSONObject().apply {
            put("type", v.type.name)
            put("message", v.message)
          })
        }
      })

      put("auditLogs", JSONArray().apply {
        s.auditLogs.forEach { a ->
          put(JSONObject().apply {
            put("id", a.id)
            put("timeLabel", a.timeLabel)
            put("who", a.who)
            put("role", a.role)
            put("action", a.action)
            put("outcome", a.outcome.name)
            put("previousHash", a.previousHash)
            put("hash", a.hash)
          })
        }
      })
    }
  }

  private fun parseSnapshot(root: JSONObject): PersistedCareLoopSnapshot {
    val settingsObj = root.optJSONObject("appSettings")
    val appSettings = if (settingsObj != null) {
      AppSettings(
        notificationSoundEnabled = settingsObj.optBoolean("notificationSoundEnabled", true),
        popupAlertsEnabled = settingsObj.optBoolean("popupAlertsEnabled", true),
        receptionistQueueSort = runCatching {
          ReceptionistQueueSort.valueOf(settingsObj.optString("receptionistQueueSort", "RISK"))
        }.getOrDefault(ReceptionistQueueSort.RISK),
        receptionistHighRiskFirst = settingsObj.optBoolean("receptionistHighRiskFirst", false),
        doctorDefaultFollowUpDays = settingsObj.optInt("doctorDefaultFollowUpDays", 7).let {
          if (it in listOf(7, 14, 30)) it else 7
        },
        doctorAiAssistantVisible = settingsObj.optBoolean("doctorAiAssistantVisible", true),
      )
    } else {
      AppSettings()
    }

    val patientsArr = root.getJSONArray("patients")
    val patients = (0 until patientsArr.length()).map { i ->
      val o = patientsArr.getJSONObject(i)
      val id = o.getInt("id")
      val assignedDoctor = DoctorId.valueOf(o.getString("assignedDoctor"))
      val pastMissed = o.getInt("pastMissedAppointments")
      val historyArr = o.optJSONArray("missedHistory")
      val history = if (historyArr != null) {
        (0 until historyArr.length()).map { idx ->
          val h = historyArr.getJSONObject(idx)
          MissedAppointmentRecord(
            id = h.getLong("id"),
            dateKey = h.getString("dateKey"),
            dateLabel = h.getString("dateLabel"),
            doctorName = h.getString("doctorName"),
          )
        }.sortedByDescending { it.dateKey }
      } else {
        DemoClinicData.createMissedHistory(id, pastMissed, assignedDoctor.displayName)
      }
      Patient(
        id = id,
        name = o.getString("name"),
        age = o.getInt("age"),
        gender = AvatarGender.valueOf(o.getString("gender")),
        assignedDoctor = assignedDoctor,
        appointmentHoursFromNow = o.getInt("appointmentHoursFromNow"),
        appointmentDateKey = o.getString("appointmentDateKey"),
        appointmentSlotIndex = o.getInt("appointmentSlotIndex"),
        appointmentTimeLabel = o.getString("appointmentTimeLabel"),
        pastMissedAppointments = pastMissed,
        missedHistory = history,
        confirmed = o.getBoolean("confirmed"),
        cancelled = o.optBoolean("cancelled", false),
        rescheduled = o.optBoolean("rescheduled", false),
        preferredContactMethod = ContactMethod.valueOf(o.getString("preferredContactMethod")),
        smsConsent = o.getBoolean("smsConsent"),
        emailConsent = o.getBoolean("emailConsent"),
        encryptedPhone = o.getString("encryptedPhone"),
        encryptedEmail = o.getString("encryptedEmail"),
        encryptedMedicalNote = o.optNullableString("encryptedMedicalNote"),
        hasPhoneContact = o.optBoolean("hasPhoneContact", true),
        hasEmailContact = o.optBoolean("hasEmailContact", true),
        isUrgent = o.optBoolean("isUrgent", false),
        doctorDecision = o.optNullableString("doctorDecision")?.let { DoctorDecision.valueOf(it) },
        attendanceOutcome = o.optNullableString("attendanceOutcome")?.let { AttendanceOutcome.valueOf(it) },
        attendanceRecordedAt = o.optNullableString("attendanceRecordedAt"),
        accountEmailOverride = o.optNullableString("accountEmailOverride"),
      )
    }

    val profilesArr = root.getJSONArray("userProfiles")
    val profiles = linkedMapOf<String, UserProfile>()
    for (i in 0 until profilesArr.length()) {
      val o = profilesArr.getJSONObject(i)
      val key = o.getString("accountKey")
      profiles[key] = UserProfile(
        accountKey = key,
        name = o.getString("name"),
        phone = o.optString("phone", ""),
        email = o.optString("email", ""),
        gender = AvatarGender.valueOf(o.getString("gender")),
        photoDataUrl = o.optNullableString("photoDataUrl"),
        doctorId = o.optNullableString("doctorId")?.let { DoctorId.valueOf(it) },
        patientId = if (o.isNull("patientId")) null else o.getInt("patientId"),
      )
    }

    val msgsArr = root.getJSONArray("inboxMessages")
    val messages = (0 until msgsArr.length()).map { i ->
      val o = msgsArr.getJSONObject(i)
      InboxMessage(
        id = o.getLong("id"),
        patientId = o.getInt("patientId"),
        recipientKey = o.optString("recipientKey", "patient_${o.getInt("patientId")}"),
        title = o.optString("title", "Clinic Notification"),
        channelLabel = o.getString("channelLabel"),
        timestampLabel = o.getString("timestampLabel"),
        text = o.getString("text"),
        isRead = o.getBoolean("isRead"),
        isReminder = o.optBoolean("isReminder", false),
        acknowledged = o.optBoolean("acknowledged", false),
        createdAtEpochMs = o.optLong("createdAtEpochMs", 0L),
        category = runCatching {
          NotificationCategory.valueOf(o.optString("category", "GENERAL"))
        }.getOrDefault(NotificationCategory.GENERAL),
        targetDoctorTab = o.optNullableString("targetDoctorTab")?.let {
          runCatching { DoctorTab.valueOf(it) }.getOrNull()
        },
        targetPatientId = if (o.isNull("targetPatientId")) null else o.optInt("targetPatientId"),
      )
    }

    val reqArr = root.getJSONArray("appointmentRequests")
    val requests = (0 until reqArr.length()).map { i ->
      val o = reqArr.getJSONObject(i)
      AppointmentRequest(
        id = o.getLong("id"),
        patientId = o.getInt("patientId"),
        patientName = o.getString("patientName"),
        doctorId = DoctorId.valueOf(o.getString("doctorId")),
        preferredDateKey = o.getString("preferredDateKey"),
        preferredDateLabel = o.getString("preferredDateLabel"),
        preferredTimeOfDay = TimeOfDay.valueOf(o.getString("preferredTimeOfDay")),
        preferredSlotIndex = if (o.isNull("preferredSlotIndex")) null else o.getInt("preferredSlotIndex"),
        reason = o.getString("reason"),
        encryptedPrivateNote = o.optNullableString("encryptedPrivateNote"),
        status = AppointmentRequestStatus.valueOf(o.getString("status")),
        offeredDateKey = o.optNullableString("offeredDateKey"),
        offeredDateLabel = o.optNullableString("offeredDateLabel"),
        offeredSlotIndex = if (o.isNull("offeredSlotIndex")) null else o.getInt("offeredSlotIndex"),
        offeredTimeLabel = o.optNullableString("offeredTimeLabel"),
        bookedDateLabel = o.optNullableString("bookedDateLabel"),
        bookedTimeLabel = o.optNullableString("bookedTimeLabel"),
        declineReason = o.optNullableString("declineReason"),
        createdAtEpochMs = o.optLong("createdAtEpochMs", System.currentTimeMillis()),
      )
    }

    val bookedArr = root.getJSONArray("bookedAppointments")
    val booked = (0 until bookedArr.length()).map { i ->
      val o = bookedArr.getJSONObject(i)
      BookedAppointment(
        id = o.getLong("id"),
        patientId = o.getInt("patientId"),
        doctorId = DoctorId.valueOf(o.getString("doctorId")),
        dateKey = o.getString("dateKey"),
        slotIndex = o.getInt("slotIndex"),
        timeLabel = o.getString("timeLabel"),
        reason = o.getString("reason"),
      )
    }

    val fuArr = root.getJSONArray("followUpAppointments")
    val followUps = (0 until fuArr.length()).map { i ->
      val o = fuArr.getJSONObject(i)
      FollowUpAppointment(
        id = o.getLong("id"),
        patientId = o.getInt("patientId"),
        doctorId = DoctorId.valueOf(o.getString("doctorId")),
        dateKey = o.getString("dateKey"),
        slotIndex = o.getInt("slotIndex"),
        timeLabel = o.getString("timeLabel"),
      )
    }

    val docFuArr = root.getJSONArray("doctorRecommendedFollowUps")
    val docFollowUps = (0 until docFuArr.length()).map { i ->
      val o = docFuArr.getJSONObject(i)
      DoctorRecommendedFollowUp(
        id = o.getLong("id"),
        patientId = o.getInt("patientId"),
        doctorId = DoctorId.valueOf(o.getString("doctorId")),
        reason = o.getString("reason"),
        dueInDays = o.getInt("dueInDays"),
        dueDateKey = o.getString("dueDateKey"),
        dueDateLabel = o.getString("dueDateLabel"),
        booked = o.optBoolean("booked", false),
      )
    }

    val missedArr = root.getJSONArray("missedFollowUpTasks")
    val missedTasks = (0 until missedArr.length()).map { i ->
      val o = missedArr.getJSONObject(i)
      val fbType = o.optNullableString("feedbackType")?.let { ReminderFeedbackType.valueOf(it) }
      val fbMsg = o.optNullableString("feedbackMessage")
      MissedFollowUpTask(
        id = o.getLong("id"),
        patientId = o.getInt("patientId"),
        patientName = o.getString("patientName"),
        missedDateLabel = o.getString("missedDateLabel"),
        done = o.optBoolean("done", false),
        feedback = if (fbType != null && fbMsg != null) ReminderFeedback(fbType, fbMsg) else null,
      )
    }

    val statusObj = root.getJSONObject("reminderStatusByPatientId")
    val statusMap = linkedMapOf<Int, ReminderDeliveryStatus>()
    statusObj.keys().forEach { k ->
      statusMap[k.toInt()] = ReminderDeliveryStatus.valueOf(statusObj.getString(k))
    }

    val fbObj = root.getJSONObject("reminderFeedbackByPatientId")
    val fbMap = linkedMapOf<Int, ReminderFeedback>()
    fbObj.keys().forEach { k ->
      val item = fbObj.getJSONObject(k)
      fbMap[k.toInt()] = ReminderFeedback(
        type = ReminderFeedbackType.valueOf(item.getString("type")),
        message = item.getString("message"),
      )
    }

    val auditArr = root.getJSONArray("auditLogs")
    val auditLogs = (0 until auditArr.length()).map { i ->
      val o = auditArr.getJSONObject(i)
      AuditLogEntry(
        id = o.getLong("id"),
        timeLabel = o.getString("timeLabel"),
        who = o.getString("who"),
        role = o.getString("role"),
        action = o.getString("action"),
        outcome = AuditOutcome.valueOf(o.getString("outcome")),
        previousHash = o.getString("previousHash"),
        hash = o.getString("hash"),
      )
    }

    return PersistedCareLoopSnapshot(
      encryptionKeyBase64 = root.getString("encryptionKeyBase64"),
      patients = patients,
      userProfiles = profiles,
      inboxMessages = messages,
      appointmentRequests = requests,
      bookedAppointments = booked,
      followUpAppointments = followUps,
      doctorRecommendedFollowUps = docFollowUps,
      missedFollowUpTasks = missedTasks,
      reminderStatusByPatientId = statusMap,
      reminderFeedbackByPatientId = fbMap,
      auditLogs = auditLogs,
      appSettings = appSettings,
      nextAuditId = root.getLong("nextAuditId"),
      nextMessageId = root.getLong("nextMessageId"),
      nextRequestId = root.getLong("nextRequestId"),
      nextBookedId = root.getLong("nextBookedId"),
      nextFollowUpId = root.getLong("nextFollowUpId"),
      nextDoctorFollowUpId = root.getLong("nextDoctorFollowUpId"),
      nextMissedTaskId = root.getLong("nextMissedTaskId"),
      revision = root.optLong("revision", 0L),
    )
  }

  private fun JSONObject.optNullableString(key: String): String? =
    if (isNull(key)) null else optString(key)

  companion object {
    private const val PREFS_NAME = "careloop_persistent_store"
    private const val KEY_STATE_JSON = "careloop_state_v2"
  }
}
