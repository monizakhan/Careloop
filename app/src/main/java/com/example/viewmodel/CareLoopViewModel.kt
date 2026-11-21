package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.CareLoopAiGateway
import com.example.ai.ClinicBackupAssistant
import com.example.ai.DoctorAssistantContext
import com.example.ai.FirebaseCareLoopAiGateway
import com.example.ai.HighRiskSummaryPatient
import com.example.ai.ReminderDraftRequest
import com.example.audio.CareLoopSoundPlayer
import com.example.data.CareLoopStorage
import com.example.data.DemoClinicData
import com.example.data.InMemoryCareLoopStorage
import com.example.data.PersistedCareLoopSnapshot
import com.example.model.AdminTab
import com.example.model.AiReminderDraft
import com.example.model.AppSettings
import com.example.model.AppointmentRequest
import com.example.model.AppointmentRequestStatus
import com.example.model.AttendanceOutcome
import com.example.model.AuditIntegrityResult
import com.example.model.AuditLogEntry
import com.example.model.AuditOutcome
import com.example.model.BookedAppointment
import com.example.model.CareLoopScreen
import com.example.model.ClinicSchedule
import com.example.model.ClinicSlot
import com.example.model.ContactMethod
import com.example.model.DecryptedContact
import com.example.model.DemoAccountItem
import com.example.model.DoctorChatMessage
import com.example.model.DoctorDailySummary
import com.example.model.DoctorDecision
import com.example.model.DoctorId
import com.example.model.DoctorRecommendedFollowUp
import com.example.model.DoctorTab
import com.example.model.FollowUpAppointment
import com.example.model.InboxMessage
import com.example.model.LoggedInUser
import com.example.model.MissedAppointmentRecord
import com.example.model.MissedFollowUpTask
import com.example.model.NotificationCategory
import com.example.model.Patient
import com.example.model.PatientTab
import com.example.model.ReceptionistQueueSort
import com.example.model.ReceptionistTab
import com.example.model.ReminderDeliveryStatus
import com.example.model.ReminderFeedback
import com.example.model.ReminderFeedbackType
import com.example.model.TimeOfDay
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.security.CareLoopCrypto
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CareLoopUiState(
  val showSplash: Boolean = true,
  val currentScreen: CareLoopScreen = CareLoopScreen.Login,
  val loggedInUser: LoggedInUser? = null,
  val loginError: String? = null,
  val accessDeniedMessage: String? = null,
  val schedulingError: String? = null,
  val profileSaved: Boolean = false,
  val settingsSaved: Boolean = false,
  val focusedDoctorPatientId: Int? = null,
  val notificationsOpen: Boolean = false,
  val topToastMessage: String? = null,
  val appSettings: AppSettings = AppSettings(),
  val patients: List<Patient> = emptyList(),
  val userProfiles: Map<String, UserProfile> = emptyMap(),
  val demoAccounts: List<DemoAccountItem> = emptyList(),
  val decryptedContactsByPatientId: Map<Int, DecryptedContact> = emptyMap(),
  val decryptedMedicalNotesByPatientId: Map<Int, String> = emptyMap(),
  val reminderFeedbackByPatientId: Map<Int, ReminderFeedback> = emptyMap(),
  val reminderStatusByPatientId: Map<Int, ReminderDeliveryStatus> = emptyMap(),
  val aiReminderDraftsByPatientId: Map<Int, AiReminderDraft> = emptyMap(),
  val aiDraftLoadingPatientIds: Set<Int> = emptySet(),
  val doctorSummaryByDoctorId: Map<DoctorId, DoctorDailySummary> = emptyMap(),
  val doctorChatMessagesByDoctorId: Map<DoctorId, List<DoctorChatMessage>> = emptyMap(),
  val doctorChatLoadingDoctorIds: Set<DoctorId> = emptySet(),
  val inboxMessages: List<InboxMessage> = emptyList(),
  val appointmentRequests: List<AppointmentRequest> = emptyList(),
  val bookedAppointments: List<BookedAppointment> = emptyList(),
  val followUpAppointments: List<FollowUpAppointment> = emptyList(),
  val doctorRecommendedFollowUps: List<DoctorRecommendedFollowUp> = emptyList(),
  val missedFollowUpTasks: List<MissedFollowUpTask> = emptyList(),
  val requestSentPatientIds: Set<Int> = emptySet(),
  val preferencesSavedPatientIds: Set<Int> = emptySet(),
  val auditLogs: List<AuditLogEntry> = emptyList(),
  val auditIntegrityResult: AuditIntegrityResult? = null,
)

class CareLoopViewModel(
  private val aiGateway: CareLoopAiGateway = FirebaseCareLoopAiGateway(),
  private val storage: CareLoopStorage = InMemoryCareLoopStorage,
) : ViewModel() {
  constructor() : this(FirebaseCareLoopAiGateway(), InMemoryCareLoopStorage)

  // Secret AES-GCM key stays strictly encapsulated inside CareLoopCrypto and persisted storage
  private var crypto: CareLoopCrypto
  private var nextAuditId = 1L
  private var nextMessageId = 1L
  private var nextRequestId = 1L
  private var nextBookedId = 1L
  private var nextFollowUpId = 1L
  private var nextDoctorFollowUpId = 2L
  private var nextMissedTaskId = 1L
  private var nextChatMessageId = 1L
  private var storageRevision = 0L
  private var unregisterExternalSync: (() -> Unit)? = null

  private val _uiState: MutableStateFlow<CareLoopUiState>
  val uiState: StateFlow<CareLoopUiState>

  init {
    val loaded = storage.load()
    if (loaded != null) {
      crypto = CareLoopCrypto(loaded.encryptionKeyBase64)
      nextAuditId = loaded.nextAuditId
      nextMessageId = loaded.nextMessageId
      nextRequestId = loaded.nextRequestId
      nextBookedId = loaded.nextBookedId
      nextFollowUpId = loaded.nextFollowUpId
      nextDoctorFollowUpId = loaded.nextDoctorFollowUpId
      nextMissedTaskId = loaded.nextMissedTaskId
      storageRevision = loaded.revision
      CareLoopSoundPlayer.isSoundEnabled = loaded.appSettings.notificationSoundEnabled
      _uiState = MutableStateFlow(
        CareLoopUiState(
          appSettings = loaded.appSettings,
          patients = loaded.patients,
          userProfiles = loaded.userProfiles,
          demoAccounts = DemoClinicData.buildDemoAccounts(loaded.patients, loaded.userProfiles),
          inboxMessages = loaded.inboxMessages,
          appointmentRequests = loaded.appointmentRequests,
          bookedAppointments = loaded.bookedAppointments,
          followUpAppointments = loaded.followUpAppointments,
          doctorRecommendedFollowUps = loaded.doctorRecommendedFollowUps,
          missedFollowUpTasks = loaded.missedFollowUpTasks,
          reminderStatusByPatientId = loaded.reminderStatusByPatientId,
          reminderFeedbackByPatientId = loaded.reminderFeedbackByPatientId,
          auditLogs = loaded.auditLogs,
        )
      )
    } else {
      crypto = CareLoopCrypto()
      CareLoopSoundPlayer.isSoundEnabled = true
      val initialPatients = DemoClinicData.createFakePatients(crypto)
      val initialProfiles = DemoClinicData.createInitialProfiles(initialPatients, crypto)
      val initialDocFollowUps = DemoClinicData.createInitialDoctorFollowUps()
      val (initialStaffNotifications, updatedMsgId) = DemoClinicData.createInitialStaffNotifications(
        patients = initialPatients,
        startId = nextMessageId,
      )
      nextMessageId = updatedMsgId
      _uiState = MutableStateFlow(
        CareLoopUiState(
          appSettings = AppSettings(),
          patients = initialPatients,
          userProfiles = initialProfiles,
          demoAccounts = DemoClinicData.buildDemoAccounts(initialPatients, initialProfiles),
          inboxMessages = initialStaffNotifications,
          doctorRecommendedFollowUps = initialDocFollowUps,
        )
      )
      persistSnapshot(_uiState.value)
    }
    uiState = _uiState.asStateFlow()
    checkStaleAppointmentRequests()

    unregisterExternalSync = storage.registerExternalChangeListener {
      reloadFromExternalStorage()
    }
  }

  override fun onCleared() {
    unregisterExternalSync?.invoke()
    super.onCleared()
  }

  private fun persistSnapshot(state: CareLoopUiState) {
    storageRevision += 1L
    storage.save(
      PersistedCareLoopSnapshot(
        encryptionKeyBase64 = crypto.exportKeyBase64(),
        appSettings = state.appSettings,
        patients = state.patients,
        userProfiles = state.userProfiles,
        inboxMessages = state.inboxMessages,
        appointmentRequests = state.appointmentRequests,
        bookedAppointments = state.bookedAppointments,
        followUpAppointments = state.followUpAppointments,
        doctorRecommendedFollowUps = state.doctorRecommendedFollowUps,
        missedFollowUpTasks = state.missedFollowUpTasks,
        reminderStatusByPatientId = state.reminderStatusByPatientId,
        reminderFeedbackByPatientId = state.reminderFeedbackByPatientId,
        auditLogs = state.auditLogs,
        nextAuditId = nextAuditId,
        nextMessageId = nextMessageId,
        nextRequestId = nextRequestId,
        nextBookedId = nextBookedId,
        nextFollowUpId = nextFollowUpId,
        nextDoctorFollowUpId = nextDoctorFollowUpId,
        nextMissedTaskId = nextMissedTaskId,
        revision = storageRevision,
      )
    )
  }

  private fun updateAndPersist(transform: (CareLoopUiState) -> CareLoopUiState) {
    var updated: CareLoopUiState? = null
    _uiState.update { current ->
      val next = transform(current)
      updated = next
      next
    }
    updated?.let { persistSnapshot(it) }
  }

  private fun reloadFromExternalStorage() {
    val loaded = storage.load() ?: return
    if (loaded.revision == storageRevision) return
    val previousIds = _uiState.value.inboxMessages.map { it.id }.toSet()
    val currentUser = _uiState.value.loggedInUser
    val newForLoggedIn = if (currentUser != null) {
      loaded.inboxMessages.firstOrNull { msg ->
        msg.id !in previousIds && !msg.isRead && isMessageForUser(msg, currentUser)
      }
    } else {
      null
    }
    storageRevision = loaded.revision
    nextAuditId = loaded.nextAuditId
    nextMessageId = loaded.nextMessageId
    nextRequestId = loaded.nextRequestId
    nextBookedId = loaded.nextBookedId
    nextFollowUpId = loaded.nextFollowUpId
    nextDoctorFollowUpId = loaded.nextDoctorFollowUpId
    nextMissedTaskId = loaded.nextMissedTaskId
    crypto = CareLoopCrypto(loaded.encryptionKeyBase64)
    CareLoopSoundPlayer.isSoundEnabled = loaded.appSettings.notificationSoundEnabled

    _uiState.update { state ->
      val nextToast = if (newForLoggedIn != null && loaded.appSettings.popupAlertsEnabled) {
        "${newForLoggedIn.title}: ${newForLoggedIn.text}"
      } else {
        state.topToastMessage
      }
      state.copy(
        appSettings = loaded.appSettings,
        patients = loaded.patients,
        userProfiles = loaded.userProfiles,
        demoAccounts = DemoClinicData.buildDemoAccounts(loaded.patients, loaded.userProfiles),
        inboxMessages = loaded.inboxMessages,
        appointmentRequests = loaded.appointmentRequests,
        bookedAppointments = loaded.bookedAppointments,
        followUpAppointments = loaded.followUpAppointments,
        doctorRecommendedFollowUps = loaded.doctorRecommendedFollowUps,
        missedFollowUpTasks = loaded.missedFollowUpTasks,
        reminderStatusByPatientId = loaded.reminderStatusByPatientId,
        reminderFeedbackByPatientId = loaded.reminderFeedbackByPatientId,
        auditLogs = loaded.auditLogs,
        topToastMessage = nextToast,
      )
    }
    if (newForLoggedIn != null && loaded.appSettings.notificationSoundEnabled) {
      CareLoopSoundPlayer.playSoftNotificationBeep()
    }
  }

  fun isMessageForUser(msg: InboxMessage, user: LoggedInUser): Boolean {
    return when (user.role) {
      UserRole.PATIENT -> msg.recipientKey == user.notificationRecipientKey
      UserRole.DOCTOR -> msg.recipientKey == user.notificationRecipientKey
      UserRole.RECEPTIONIST -> msg.recipientKey == "receptionist"
      UserRole.ADMIN -> msg.recipientKey == "admin"
    }
  }

  fun notificationsForCurrentUser(state: CareLoopUiState = _uiState.value): List<InboxMessage> {
    val user = state.loggedInUser ?: return emptyList()
    return state.inboxMessages
      .filter { isMessageForUser(it, user) }
      .sortedByDescending { it.id }
  }

  private fun notifyIfForCurrentUser(
    state: CareLoopUiState,
    msg: InboxMessage,
  ): String? {
    val user = state.loggedInUser
    return if (user != null && isMessageForUser(msg, user)) {
      if (state.appSettings.notificationSoundEnabled) {
        CareLoopSoundPlayer.playSoftNotificationBeep()
      }
      if (state.appSettings.popupAlertsEnabled) {
        "${msg.title}: ${msg.text}"
      } else {
        state.topToastMessage
      }
    } else {
      state.topToastMessage
    }
  }

  private fun notifyAnyForCurrentUser(
    state: CareLoopUiState,
    messages: List<InboxMessage>,
  ): String? {
    var toast = state.topToastMessage
    for (msg in messages) {
      val next = notifyIfForCurrentUser(state, msg)
      if (next != state.topToastMessage) {
        toast = next
      }
    }
    return toast
  }

  private fun createDoctorAlert(
    doctorId: DoctorId,
    targetPatientId: Int?,
    title: String,
    text: String,
    category: NotificationCategory,
    targetTab: DoctorTab,
  ): InboxMessage {
    return InboxMessage(
      id = nextMessageId++,
      patientId = 0,
      recipientKey = DemoClinicData.doctorAccountKey(doctorId),
      title = title,
      channelLabel = "Doctor Alert",
      timestampLabel = currentDateTimeLabel(),
      text = text,
      isRead = false,
      isReminder = false,
      acknowledged = false,
      category = category,
      targetDoctorTab = targetTab,
      targetPatientId = targetPatientId,
    )
  }

  private fun createAdminAlert(
    title: String,
    text: String,
    category: NotificationCategory,
    targetPatientId: Int? = null,
  ): InboxMessage {
    return InboxMessage(
      id = nextMessageId++,
      patientId = 0,
      recipientKey = "admin",
      title = title,
      channelLabel = "System Event",
      timestampLabel = currentDateTimeLabel(),
      text = text,
      isRead = false,
      isReminder = false,
      acknowledged = false,
      category = category,
      targetPatientId = targetPatientId,
    )
  }

  fun checkStaleAppointmentRequests(nowEpochMs: Long = System.currentTimeMillis()) {
    val twentyFourHoursMs = 24L * 60L * 60L * 1000L
    val state = _uiState.value
    val staleRequests = state.appointmentRequests.filter { req ->
      req.status == AppointmentRequestStatus.PENDING &&
        (nowEpochMs - req.createdAtEpochMs) >= twentyFourHoursMs &&
        state.inboxMessages.none { msg ->
          msg.recipientKey == "admin" &&
            msg.category == NotificationCategory.STALE_REQUEST &&
            msg.targetPatientId == req.patientId &&
            msg.text.contains("#${req.id}")
        }
    }
    if (staleRequests.isEmpty()) return

    updateAndPersist { current ->
      val newAlerts = staleRequests.map { req ->
        createAdminAlert(
          title = "Request waiting 24h+",
          text = "Appointment request #${req.id} for ${req.patientName} has waited more than 24 hours.",
          category = NotificationCategory.STALE_REQUEST,
          targetPatientId = req.patientId,
        )
      }
      current.copy(
        inboxMessages = newAlerts + current.inboxMessages,
        topToastMessage = notifyAnyForCurrentUser(current, newAlerts),
      )
    }
  }

  fun finishSplash() {
    _uiState.update { if (it.showSplash) it.copy(showSplash = false) else it }
  }

  fun dismissTopToast() {
    _uiState.update { if (it.topToastMessage != null) it.copy(topToastMessage = null) else it }
  }

  fun openNotificationsPanel() {
    _uiState.update { it.copy(notificationsOpen = true) }
  }

  fun closeNotificationsPanel() {
    _uiState.update { it.copy(notificationsOpen = false) }
  }

  private fun currentTimeLabel(): String =
    SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())

  private fun currentRecordedTimeLabel(): String =
    SimpleDateFormat("h:mm a", Locale.US).format(Date())

  private fun currentDateTimeLabel(): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.US).format(Date())

  private fun appendAuditEntry(
    existingLogs: List<AuditLogEntry>,
    who: String,
    role: String,
    action: String,
    outcome: AuditOutcome,
  ): List<AuditLogEntry> {
    val previousHash = existingLogs.firstOrNull()?.hash ?: CareLoopCrypto.GENESIS_HASH
    val id = nextAuditId++
    val timeLabel = currentTimeLabel()
    val hash = CareLoopCrypto.computeAuditEntryHash(
      id = id,
      timeLabel = timeLabel,
      who = who,
      role = role,
      action = action,
      outcome = outcome,
      previousHash = previousHash,
    )
    val entry = AuditLogEntry(
      id = id,
      timeLabel = timeLabel,
      who = who,
      role = role,
      action = action,
      outcome = outcome,
      previousHash = previousHash,
      hash = hash,
    )
    return listOf(entry) + existingLogs
  }

  /**
   * Role-checked decryption for a patient's medical note.
   * Allowed ONLY for the patient's assigned Doctor.
   * Logs every decryption attempt as allowed or denied in the audit log.
   */
  fun attemptDecryptMedicalNote(patientId: Int): String? {
    var result: String? = null
    updateAndPersist { state ->
      val user = state.loggedInUser
      val patient = state.patients.find { it.id == patientId } ?: return@updateAndPersist state
      val encryptedNote = patient.encryptedMedicalNote
      val allowedDoctor = user?.role == UserRole.DOCTOR && user.doctorId == patient.assignedDoctor

      if (allowedDoctor && encryptedNote != null) {
        val decrypted = crypto.decrypt(encryptedNote)
        result = decrypted
        val logs = appendAuditEntry(
          existingLogs = state.auditLogs,
          who = user!!.displayName,
          role = user.roleLabel,
          action = "Decrypted medical note for ${patient.name} (AES-GCM)",
          outcome = AuditOutcome.ALLOWED,
        )
        state.copy(
          decryptedMedicalNotesByPatientId = state.decryptedMedicalNotesByPatientId + (patient.id to decrypted),
          auditLogs = logs,
          auditIntegrityResult = null,
        )
      } else {
        val actor = user?.displayName ?: "Unknown"
        val actorRole = user?.roleLabel ?: "Unauthenticated"
        val logs = appendAuditEntry(
          existingLogs = state.auditLogs,
          who = actor,
          role = actorRole,
          action = "Denied decryption of medical note for ${patient.name}",
          outcome = AuditOutcome.DENIED,
        )
        val adminAlert = createAdminAlert(
          title = "Access denied",
          text = "An access attempt to a medical note was denied for $actor.",
          category = NotificationCategory.ACCESS_DENIED,
          targetPatientId = patient.id,
        )
        state.copy(
          inboxMessages = listOf(adminAlert) + state.inboxMessages,
          topToastMessage = notifyIfForCurrentUser(state, adminAlert),
          auditLogs = logs,
          auditIntegrityResult = null,
        )
      }
    }
    return result
  }

  /**
   * Role-checked decryption for a patient's phone and email contact details.
   * Allowed ONLY for Receptionist or that Patient themselves.
   * Logs every decryption attempt as allowed or denied in the audit log.
   */
  fun attemptDecryptContact(patientId: Int): DecryptedContact? {
    var result: DecryptedContact? = null
    updateAndPersist { state ->
      val user = state.loggedInUser
      val patient = state.patients.find { it.id == patientId } ?: return@updateAndPersist state
      val allowed = when (user?.role) {
        UserRole.RECEPTIONIST -> true
        UserRole.PATIENT -> user.patientId == patient.id
        else -> false
      }

      if (user != null && allowed) {
        val contact = DecryptedContact(
          phone = if (patient.hasPhoneContact && patient.encryptedPhone.isNotBlank()) crypto.decrypt(patient.encryptedPhone) else "",
          email = if (patient.hasEmailContact && patient.encryptedEmail.isNotBlank()) crypto.decrypt(patient.encryptedEmail) else "",
        )
        result = contact
        val logs = appendAuditEntry(
          existingLogs = state.auditLogs,
          who = user.displayName,
          role = user.roleLabel,
          action = "Decrypted contact details for ${patient.name} (AES-GCM)",
          outcome = AuditOutcome.ALLOWED,
        )
        state.copy(
          decryptedContactsByPatientId = state.decryptedContactsByPatientId + (patient.id to contact),
          auditLogs = logs,
          auditIntegrityResult = null,
        )
      } else {
        val actor = user?.displayName ?: "Unknown"
        val actorRole = user?.roleLabel ?: "Unauthenticated"
        val logs = appendAuditEntry(
          existingLogs = state.auditLogs,
          who = actor,
          role = actorRole,
          action = "Denied decryption of contact details for ${patient.name}",
          outcome = AuditOutcome.DENIED,
        )
        val adminAlert = createAdminAlert(
          title = "Access denied",
          text = "An access attempt to contact details was denied for $actor.",
          category = NotificationCategory.ACCESS_DENIED,
          targetPatientId = patient.id,
        )
        state.copy(
          inboxMessages = listOf(adminAlert) + state.inboxMessages,
          topToastMessage = notifyIfForCurrentUser(state, adminAlert),
          auditLogs = logs,
          auditIntegrityResult = null,
        )
      }
    }
    return result
  }

  fun verifyAuditLogIntegrity() {
    updateAndPersist { state ->
      val chronological = state.auditLogs.sortedBy { it.id }
      var expectedPreviousHash = CareLoopCrypto.GENESIS_HASH
      var tamperedEntry: AuditLogEntry? = null

      for (entry in chronological) {
        val expectedHash = CareLoopCrypto.computeAuditEntryHash(
          id = entry.id,
          timeLabel = entry.timeLabel,
          who = entry.who,
          role = entry.role,
          action = entry.action,
          outcome = entry.outcome,
          previousHash = entry.previousHash,
        )
        if (entry.previousHash != expectedPreviousHash || entry.hash != expectedHash) {
          tamperedEntry = entry
          break
        }
        expectedPreviousHash = entry.hash
      }

      val result = if (tamperedEntry == null) {
        AuditIntegrityResult(
          verified = true,
          message = "Audit log verified, no changes detected",
        )
      } else {
        AuditIntegrityResult(
          verified = false,
          message = "Change detected at Entry #${tamperedEntry.id} (${tamperedEntry.timeLabel} · ${tamperedEntry.action})",
        )
      }
      val adminAlert = createAdminAlert(
        title = if (result.verified) "Audit integrity passed" else "Audit integrity failed",
        text = if (result.verified) {
          "Audit integrity check passed with no tampering detected."
        } else {
          "Audit integrity check failed at Entry #${tamperedEntry?.id}."
        },
        category = NotificationCategory.AUDIT_INTEGRITY,
      )
      state.copy(
        auditIntegrityResult = result,
        inboxMessages = listOf(adminAlert) + state.inboxMessages,
        topToastMessage = notifyIfForCurrentUser(state, adminAlert),
      )
    }
  }

  internal fun tamperAuditEntryForTest(entryId: Long, tamperedAction: String) {
    _uiState.update { state ->
      val updated = state.auditLogs.map { entry ->
        if (entry.id == entryId) entry.copy(action = tamperedAction) else entry
      }
      state.copy(auditLogs = updated, auditIntegrityResult = null)
    }
  }

  fun isSlotTaken(
    state: CareLoopUiState = _uiState.value,
    doctorId: DoctorId,
    dateKey: String,
    slotIndex: Int,
  ): Boolean {
    val takenByPatient = state.patients.any { p ->
      p.assignedDoctor == doctorId &&
        !p.cancelled &&
        p.attendanceOutcome != AttendanceOutcome.NO_SHOW &&
        p.appointmentDateKey == dateKey &&
        p.appointmentSlotIndex == slotIndex
    }
    if (takenByPatient) return true

    val takenByBooked = state.bookedAppointments.any { b ->
      b.doctorId == doctorId && b.dateKey == dateKey && b.slotIndex == slotIndex
    }
    if (takenByBooked) return true

    return state.followUpAppointments.any { f ->
      f.doctorId == doctorId && f.dateKey == dateKey && f.slotIndex == slotIndex
    }
  }

  fun freeSlotsFor(
    doctorId: DoctorId,
    dateKey: String,
    timeOfDay: TimeOfDay? = null,
    state: CareLoopUiState = _uiState.value,
  ): List<ClinicSlot> {
    if (ClinicSchedule.isPastDateKey(dateKey) || ClinicSchedule.isSundayDateKey(dateKey)) {
      return emptyList()
    }
    val base = if (timeOfDay != null) {
      ClinicSchedule.slotsForTimeOfDay(timeOfDay)
    } else {
      ClinicSchedule.ALL_SLOTS
    }
    return base.filter { slot -> !isSlotTaken(state, doctorId, dateKey, slot.index) }
  }

  fun clearLoginError() {
    _uiState.update { if (it.loginError != null) it.copy(loginError = null) else it }
  }

  fun clearSchedulingError() {
    _uiState.update { if (it.schedulingError != null) it.copy(schedulingError = null) else it }
  }

  fun clearProfileSaved() {
    _uiState.update { if (it.profileSaved) it.copy(profileSaved = false) else it }
  }

  fun clearSettingsSaved() {
    _uiState.update { if (it.settingsSaved) it.copy(settingsSaved = false) else it }
  }

  fun clearFocusedDoctorPatient() {
    _uiState.update { if (it.focusedDoctorPatientId != null) it.copy(focusedDoctorPatientId = null) else it }
  }

  fun signIn(rawEmail: String, rawPassword: String) {
    val email = rawEmail.trim().lowercase(Locale.US)
    val password = rawPassword

    updateAndPersist { state ->
      if (email.isEmpty() || password.isEmpty()) {
        val logs = appendAuditEntry(
          existingLogs = state.auditLogs,
          who = email.ifEmpty { "Unknown" },
          role = "Unauthenticated",
          action = "Failed login attempt (missing credentials)",
          outcome = AuditOutcome.DENIED,
        )
        val adminAlert = createAdminAlert(
          title = "Access denied",
          text = "A sign-in attempt with missing credentials was denied.",
          category = NotificationCategory.ACCESS_DENIED,
        )
        return@updateAndPersist state.copy(
          loginError = "Invalid email or password.",
          inboxMessages = listOf(adminAlert) + state.inboxMessages,
          auditLogs = logs,
          auditIntegrityResult = null,
        )
      }

      if (password != DemoClinicData.DEMO_PASSWORD) {
        val logs = appendAuditEntry(
          existingLogs = state.auditLogs,
          who = email,
          role = "Unauthenticated",
          action = "Failed login attempt ($email)",
          outcome = AuditOutcome.DENIED,
        )
        val adminAlert = createAdminAlert(
          title = "Access denied",
          text = "A sign-in attempt for $email was denied.",
          category = NotificationCategory.ACCESS_DENIED,
        )
        return@updateAndPersist state.copy(
          loginError = "Invalid email or password.",
          inboxMessages = listOf(adminAlert) + state.inboxMessages,
          auditLogs = logs,
          auditIntegrityResult = null,
        )
      }

      val matchedProfile = state.userProfiles.values.firstOrNull {
        it.email.trim().lowercase(Locale.US) == email
      }

      val matchedUserAndScreen: Pair<LoggedInUser, CareLoopScreen>? = when {
        matchedProfile?.accountKey == DemoClinicData.ACCOUNT_RECEPTIONIST ||
          email == DemoClinicData.RECEPTIONIST_EMAIL -> {
          val profile = matchedProfile ?: state.userProfiles[DemoClinicData.ACCOUNT_RECEPTIONIST]
          LoggedInUser(
            accountKey = DemoClinicData.ACCOUNT_RECEPTIONIST,
            email = email,
            displayName = profile?.name ?: "Clinic Desk",
            roleLabel = "Receptionist",
            role = UserRole.RECEPTIONIST,
          ) to CareLoopScreen.Receptionist()
        }

        matchedProfile?.doctorId != null -> {
          val docId = matchedProfile.doctorId
          LoggedInUser(
            accountKey = matchedProfile.accountKey,
            email = email,
            displayName = matchedProfile.name,
            roleLabel = "${docId.specializationLabel}",
            role = UserRole.DOCTOR,
            doctorId = docId,
          ) to CareLoopScreen.Doctor(docId)
        }

        DoctorId.entries.any { it.defaultEmail == email } -> {
          val docId = DoctorId.entries.first { it.defaultEmail == email }
          val key = DemoClinicData.doctorAccountKey(docId)
          val profile = state.userProfiles[key]
          LoggedInUser(
            accountKey = key,
            email = email,
            displayName = profile?.name ?: docId.displayName,
            roleLabel = docId.specializationLabel,
            role = UserRole.DOCTOR,
            doctorId = docId,
          ) to CareLoopScreen.Doctor(docId)
        }

        matchedProfile?.accountKey == DemoClinicData.ACCOUNT_ADMIN ||
          email == DemoClinicData.ADMIN_EMAIL -> {
          val profile = matchedProfile ?: state.userProfiles[DemoClinicData.ACCOUNT_ADMIN]
          LoggedInUser(
            accountKey = DemoClinicData.ACCOUNT_ADMIN,
            email = email,
            displayName = profile?.name ?: "Admin",
            roleLabel = "Admin",
            role = UserRole.ADMIN,
          ) to CareLoopScreen.Admin(AdminTab.OVERVIEW)
        }

        else -> {
          val patientByProfile = matchedProfile?.patientId?.let { pid ->
            state.patients.find { it.id == pid }
          }
          val patient = patientByProfile ?: state.patients.find { it.demoEmail.lowercase(Locale.US) == email }
          if (patient != null) {
            val key = DemoClinicData.patientAccountKey(patient.id)
            LoggedInUser(
              accountKey = key,
              email = email,
              displayName = patient.name,
              roleLabel = "Patient",
              role = UserRole.PATIENT,
              patientId = patient.id,
            ) to CareLoopScreen.PatientPortal(patient.id)
          } else {
            null
          }
        }
      }

      if (matchedUserAndScreen == null) {
        val logs = appendAuditEntry(
          existingLogs = state.auditLogs,
          who = email,
          role = "Unauthenticated",
          action = "Failed login attempt ($email)",
          outcome = AuditOutcome.DENIED,
        )
        val adminAlert = createAdminAlert(
          title = "Access denied",
          text = "An unrecognized account sign-in attempt ($email) was denied.",
          category = NotificationCategory.ACCESS_DENIED,
        )
        return@updateAndPersist state.copy(
          loginError = "Invalid email or password.",
          inboxMessages = listOf(adminAlert) + state.inboxMessages,
          auditLogs = logs,
          auditIntegrityResult = null,
        )
      }

      val (user, targetScreen) = matchedUserAndScreen
      var logs = appendAuditEntry(
        existingLogs = state.auditLogs,
        who = user.displayName,
        role = user.roleLabel,
        action = "Signed in",
        outcome = AuditOutcome.ALLOWED,
      )

      // Role-checked AES-GCM decryption strictly for what this role may view
      var contactsMap = emptyMap<Int, DecryptedContact>()
      var notesMap = emptyMap<Int, String>()

      when (user.role) {
        UserRole.RECEPTIONIST -> {
          contactsMap = state.patients.associate { p ->
            p.id to DecryptedContact(
              phone = if (p.hasPhoneContact && p.encryptedPhone.isNotBlank()) crypto.decrypt(p.encryptedPhone) else "",
              email = if (p.hasEmailContact && p.encryptedEmail.isNotBlank()) crypto.decrypt(p.encryptedEmail) else "",
            )
          }
          logs = appendAuditEntry(
            existingLogs = logs,
            who = user.displayName,
            role = user.roleLabel,
            action = "Decrypted patient contact details (AES-GCM)",
            outcome = AuditOutcome.ALLOWED,
          )
        }

        UserRole.DOCTOR -> {
          val docId = user.doctorId
          if (docId != null) {
            notesMap = state.patients
              .filter { it.assignedDoctor == docId && it.encryptedMedicalNote != null }
              .associate { p ->
                p.id to crypto.decrypt(p.encryptedMedicalNote!!)
              }
            logs = appendAuditEntry(
              existingLogs = logs,
              who = user.displayName,
              role = user.roleLabel,
              action = "Decrypted medical notes for ${docId.displayName}'s patients (AES-GCM)",
              outcome = AuditOutcome.ALLOWED,
            )
          }
        }

        UserRole.PATIENT -> {
          val patient = state.patients.find { it.id == user.patientId }
          if (patient != null) {
            contactsMap = mapOf(
              patient.id to DecryptedContact(
                phone = if (patient.hasPhoneContact && patient.encryptedPhone.isNotBlank()) crypto.decrypt(patient.encryptedPhone) else "",
                email = if (patient.hasEmailContact && patient.encryptedEmail.isNotBlank()) crypto.decrypt(patient.encryptedEmail) else "",
              )
            )
            logs = appendAuditEntry(
              existingLogs = logs,
              who = user.displayName,
              role = user.roleLabel,
              action = "Decrypted contact details for ${patient.name} (AES-GCM)",
              outcome = AuditOutcome.ALLOWED,
            )
          }
        }

        UserRole.ADMIN -> {
          // Admin never decrypts medical notes or contact details
        }
      }

      state.copy(
        currentScreen = targetScreen,
        loggedInUser = user,
        loginError = null,
        accessDeniedMessage = null,
        schedulingError = null,
        profileSaved = false,
        settingsSaved = false,
        focusedDoctorPatientId = null,
        notificationsOpen = false,
        topToastMessage = null,
        decryptedContactsByPatientId = contactsMap,
        decryptedMedicalNotesByPatientId = notesMap,
        aiReminderDraftsByPatientId = emptyMap(),
        aiDraftLoadingPatientIds = emptySet(),
        doctorSummaryByDoctorId = emptyMap(),
        requestSentPatientIds = emptySet(),
        preferencesSavedPatientIds = emptySet(),
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun logout() {
    updateAndPersist { state ->
      val user = state.loggedInUser
      val updatedLogs = if (user != null) {
        appendAuditEntry(
          existingLogs = state.auditLogs,
          who = user.displayName,
          role = user.roleLabel,
          action = "Logged out",
          outcome = AuditOutcome.ALLOWED,
        )
      } else {
        state.auditLogs
      }
      state.copy(
        currentScreen = CareLoopScreen.Login,
        loggedInUser = null,
        loginError = null,
        accessDeniedMessage = null,
        schedulingError = null,
        profileSaved = false,
        settingsSaved = false,
        focusedDoctorPatientId = null,
        notificationsOpen = false,
        topToastMessage = null,
        decryptedContactsByPatientId = emptyMap(),
        decryptedMedicalNotesByPatientId = emptyMap(),
        aiReminderDraftsByPatientId = emptyMap(),
        aiDraftLoadingPatientIds = emptySet(),
        doctorSummaryByDoctorId = emptyMap(),
        requestSentPatientIds = emptySet(),
        preferencesSavedPatientIds = emptySet(),
        auditLogs = updatedLogs,
        auditIntegrityResult = null,
      )
    }
  }

  fun openPrimaryRolePage() {
    _uiState.update { state ->
      val user = state.loggedInUser ?: return@update state
      val target = when (user.role) {
        UserRole.RECEPTIONIST -> CareLoopScreen.Receptionist()
        UserRole.DOCTOR -> CareLoopScreen.Doctor(user.doctorId ?: DoctorId.ALI_KHAN)
        UserRole.PATIENT -> CareLoopScreen.PatientPortal(user.patientId ?: state.patients.first().id)
        UserRole.ADMIN -> CareLoopScreen.Admin(AdminTab.OVERVIEW)
      }
      state.copy(
        currentScreen = target,
        accessDeniedMessage = null,
        schedulingError = null,
        profileSaved = false,
      )
    }
  }

  fun selectPatientTab(tab: PatientTab) {
    _uiState.update { state ->
      val user = state.loggedInUser ?: return@update state
      if (user.role != UserRole.PATIENT) return@update state
      val pid = user.patientId ?: state.patients.first().id
      state.copy(
        currentScreen = CareLoopScreen.PatientPortal(pid, tab),
        accessDeniedMessage = null,
        schedulingError = null,
      )
    }
  }

  fun selectReceptionistTab(tab: ReceptionistTab) {
    _uiState.update { state ->
      val user = state.loggedInUser ?: return@update state
      if (user.role != UserRole.RECEPTIONIST) return@update state
      state.copy(
        currentScreen = CareLoopScreen.Receptionist(tab),
        accessDeniedMessage = null,
        schedulingError = null,
      )
    }
  }

  fun selectDoctorTab(tab: DoctorTab) {
    _uiState.update { state ->
      val user = state.loggedInUser ?: return@update state
      if (user.role != UserRole.DOCTOR) return@update state
      val docId = user.doctorId ?: DoctorId.ALI_KHAN
      state.copy(
        currentScreen = CareLoopScreen.Doctor(docId, tab),
        focusedDoctorPatientId = if (tab == DoctorTab.PATIENTS) state.focusedDoctorPatientId else null,
        accessDeniedMessage = null,
        schedulingError = null,
      )
    }
  }

  fun selectAdminTab(tab: AdminTab) {
    updateAndPersist { state ->
      val user = state.loggedInUser ?: return@updateAndPersist state
      if (user.role != UserRole.ADMIN) {
        val logs = appendAuditEntry(
          existingLogs = state.auditLogs,
          who = user.displayName,
          role = user.roleLabel,
          action = "Attempted to open Admin ${tab.label}",
          outcome = AuditOutcome.DENIED,
        )
        val adminAlert = createAdminAlert(
          title = "Access denied",
          text = "${user.displayName} (${user.roleLabel}) was denied access to Admin ${tab.label}.",
          category = NotificationCategory.ACCESS_DENIED,
        )
        return@updateAndPersist state.copy(
          accessDeniedMessage = "Access denied",
          inboxMessages = listOf(adminAlert) + state.inboxMessages,
          auditLogs = logs,
          auditIntegrityResult = null,
        )
      }
      state.copy(
        currentScreen = CareLoopScreen.Admin(tab),
        settingsSaved = false,
        accessDeniedMessage = null,
        schedulingError = null,
      )
    }
  }

  fun openAuditLog() = selectAdminTab(AdminTab.AUDIT_LOG)

  fun openAdminSecurity() = selectAdminTab(AdminTab.SECURITY)

  fun openProfile() {
    _uiState.update { state ->
      if (state.loggedInUser == null) return@update state
      val returnTo = when (val cur = state.currentScreen) {
        is CareLoopScreen.Profile -> cur.returnScreen
        is CareLoopScreen.Settings -> cur.returnScreen
        else -> cur
      }
      state.copy(
        currentScreen = CareLoopScreen.Profile(returnTo),
        profileSaved = false,
        settingsSaved = false,
        notificationsOpen = false,
      )
    }
  }

  fun openSettings() {
    _uiState.update { state ->
      val user = state.loggedInUser ?: return@update state
      if (user.role == UserRole.ADMIN) {
        return@update state.copy(
          currentScreen = CareLoopScreen.Admin(AdminTab.SETTINGS),
          settingsSaved = false,
          notificationsOpen = false,
        )
      }
      val returnTo = when (val cur = state.currentScreen) {
        is CareLoopScreen.Profile -> cur.returnScreen
        is CareLoopScreen.Settings -> cur.returnScreen
        else -> cur
      }
      state.copy(
        currentScreen = CareLoopScreen.Settings(returnTo),
        settingsSaved = false,
        notificationsOpen = false,
      )
    }
  }

  fun navigateBackFromSubScreen() {
    _uiState.update { state ->
      val target = when (val cur = state.currentScreen) {
        is CareLoopScreen.Profile -> cur.returnScreen
        is CareLoopScreen.Settings -> cur.returnScreen
        else -> cur
      }
      state.copy(currentScreen = target, profileSaved = false, settingsSaved = false)
    }
  }

  fun updateNotificationSound(enabled: Boolean) {
    CareLoopSoundPlayer.isSoundEnabled = enabled
    updateAndPersist { state ->
      if (state.appSettings.notificationSoundEnabled == enabled) return@updateAndPersist state
      val user = state.loggedInUser
      val logs = appendAuditEntry(
        existingLogs = state.auditLogs,
        who = user?.displayName ?: "User",
        role = user?.roleLabel ?: "Staff",
        action = "Settings updated: Notification sound ${if (enabled) "On" else "Off"}",
        outcome = AuditOutcome.ALLOWED,
      )
      state.copy(
        appSettings = state.appSettings.copy(notificationSoundEnabled = enabled),
        settingsSaved = true,
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun updatePopupAlerts(enabled: Boolean) {
    updateAndPersist { state ->
      if (state.appSettings.popupAlertsEnabled == enabled) return@updateAndPersist state
      val user = state.loggedInUser
      val logs = appendAuditEntry(
        existingLogs = state.auditLogs,
        who = user?.displayName ?: "User",
        role = user?.roleLabel ?: "Staff",
        action = "Settings updated: Pop-up alerts ${if (enabled) "On" else "Off"}",
        outcome = AuditOutcome.ALLOWED,
      )
      state.copy(
        appSettings = state.appSettings.copy(popupAlertsEnabled = enabled),
        topToastMessage = if (!enabled) null else state.topToastMessage,
        settingsSaved = true,
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun updateReceptionistQueueSort(sort: ReceptionistQueueSort) {
    updateAndPersist { state ->
      if (state.appSettings.receptionistQueueSort == sort) return@updateAndPersist state
      val user = state.loggedInUser
      val logs = appendAuditEntry(
        existingLogs = state.auditLogs,
        who = user?.displayName ?: "Clinic Desk",
        role = user?.roleLabel ?: "Receptionist",
        action = "Settings updated: Default queue sort set to ${sort.label}",
        outcome = AuditOutcome.ALLOWED,
      )
      state.copy(
        appSettings = state.appSettings.copy(receptionistQueueSort = sort),
        settingsSaved = true,
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun updateReceptionistHighRiskFirst(enabled: Boolean) {
    updateAndPersist { state ->
      if (state.appSettings.receptionistHighRiskFirst == enabled) return@updateAndPersist state
      val user = state.loggedInUser
      val logs = appendAuditEntry(
        existingLogs = state.auditLogs,
        who = user?.displayName ?: "Clinic Desk",
        role = user?.roleLabel ?: "Receptionist",
        action = "Settings updated: Show only high-risk first ${if (enabled) "On" else "Off"}",
        outcome = AuditOutcome.ALLOWED,
      )
      state.copy(
        appSettings = state.appSettings.copy(receptionistHighRiskFirst = enabled),
        settingsSaved = true,
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun updateDoctorDefaultFollowUpDays(days: Int) {
    val validDays = if (days in listOf(7, 14, 30)) days else 7
    updateAndPersist { state ->
      if (state.appSettings.doctorDefaultFollowUpDays == validDays) return@updateAndPersist state
      val user = state.loggedInUser
      val logs = appendAuditEntry(
        existingLogs = state.auditLogs,
        who = user?.displayName ?: "Doctor",
        role = user?.roleLabel ?: "Doctor",
        action = "Settings updated: Default follow-up due set to $validDays days",
        outcome = AuditOutcome.ALLOWED,
      )
      state.copy(
        appSettings = state.appSettings.copy(doctorDefaultFollowUpDays = validDays),
        settingsSaved = true,
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun updateDoctorAiAssistantVisible(visible: Boolean) {
    updateAndPersist { state ->
      if (state.appSettings.doctorAiAssistantVisible == visible) return@updateAndPersist state
      val user = state.loggedInUser
      val logs = appendAuditEntry(
        existingLogs = state.auditLogs,
        who = user?.displayName ?: "Doctor",
        role = user?.roleLabel ?: "Doctor",
        action = "Settings updated: AI assistant ${if (visible) "Shown" else "Hidden"}",
        outcome = AuditOutcome.ALLOWED,
      )
      state.copy(
        appSettings = state.appSettings.copy(doctorAiAssistantVisible = visible),
        settingsSaved = true,
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun updateUserProfile(
    name: String,
    phone: String,
    email: String,
    photoDataUrl: String?,
  ) {
    val cleanName = name.trim()
    val cleanPhone = phone.trim()
    val cleanEmail = email.trim().lowercase(Locale.US)
    if (cleanName.isEmpty() || cleanEmail.isEmpty()) return

    updateAndPersist { state ->
      val user = state.loggedInUser ?: return@updateAndPersist state
      val existing = state.userProfiles[user.accountKey] ?: return@updateAndPersist state
      val updatedProfile = existing.copy(
        name = cleanName,
        phone = cleanPhone,
        email = cleanEmail,
        photoDataUrl = photoDataUrl,
      )
      val updatedProfiles = state.userProfiles + (user.accountKey to updatedProfile)

      var updatedPatients = state.patients
      var updatedContacts = state.decryptedContactsByPatientId
      if (user.role == UserRole.PATIENT && user.patientId != null) {
        updatedPatients = state.patients.map { p ->
          if (p.id == user.patientId) {
            p.copy(
              name = cleanName,
              encryptedPhone = if (cleanPhone.isNotBlank()) crypto.encrypt(cleanPhone) else "",
              encryptedEmail = if (cleanEmail.isNotBlank()) crypto.encrypt(cleanEmail) else "",
              hasPhoneContact = cleanPhone.isNotBlank(),
              hasEmailContact = cleanEmail.isNotBlank(),
              accountEmailOverride = cleanEmail,
            )
          } else {
            p
          }
        }
        updatedContacts = updatedContacts + (
          user.patientId to DecryptedContact(phone = cleanPhone, email = cleanEmail)
        )
      }

      val updatedUser = user.copy(
        displayName = cleanName,
        email = cleanEmail,
      )
      val logs = appendAuditEntry(
        existingLogs = state.auditLogs,
        who = cleanName,
        role = user.roleLabel,
        action = "Profile updated ($cleanName, $cleanEmail)",
        outcome = AuditOutcome.ALLOWED,
      )

      state.copy(
        loggedInUser = updatedUser,
        patients = updatedPatients,
        userProfiles = updatedProfiles,
        demoAccounts = DemoClinicData.buildDemoAccounts(updatedPatients, updatedProfiles),
        decryptedContactsByPatientId = updatedContacts,
        profileSaved = true,
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun acknowledgeNotification(messageId: Long) {
    updateAndPersist { state ->
      val msg = state.inboxMessages.find { it.id == messageId } ?: return@updateAndPersist state
      val user = state.loggedInUser
      val updatedMessages = state.inboxMessages.map {
        if (it.id == messageId) it.copy(isRead = true, acknowledged = true) else it
      }

      var updatedStatuses = state.reminderStatusByPatientId
      var logs = state.auditLogs
      if (msg.isReminder && !msg.acknowledged) {
        updatedStatuses = updatedStatuses + (msg.patientId to ReminderDeliveryStatus.ACKNOWLEDGED)
        val patientName = state.patients.find { it.id == msg.patientId }?.name ?: (user?.displayName ?: "Patient")
        logs = appendAuditEntry(
          existingLogs = logs,
          who = patientName,
          role = user?.roleLabel ?: "Patient",
          action = "Reminder acknowledged (Got it) by $patientName",
          outcome = AuditOutcome.ALLOWED,
        )
      } else if (!msg.isRead) {
        logs = appendAuditEntry(
          existingLogs = logs,
          who = user?.displayName ?: "User",
          role = user?.roleLabel ?: "User",
          action = "Notification marked read: ${msg.title}",
          outcome = AuditOutcome.ALLOWED,
        )
      }

      state.copy(
        inboxMessages = updatedMessages,
        reminderStatusByPatientId = updatedStatuses,
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun openNotificationTarget(messageId: Long) {
    updateAndPersist { state ->
      val msg = state.inboxMessages.find { it.id == messageId } ?: return@updateAndPersist state
      val user = state.loggedInUser
      val updatedMessages = state.inboxMessages.map {
        if (it.id == messageId) it.copy(isRead = true) else it
      }
      val logs = if (!msg.isRead) {
        appendAuditEntry(
          existingLogs = state.auditLogs,
          who = user?.displayName ?: "User",
          role = user?.roleLabel ?: "User",
          action = "Notification opened: ${msg.title}",
          outcome = AuditOutcome.ALLOWED,
        )
      } else {
        state.auditLogs
      }

      if (user?.role == UserRole.DOCTOR && msg.targetDoctorTab != null) {
        val docId = user.doctorId ?: DoctorId.ALI_KHAN
        state.copy(
          inboxMessages = updatedMessages,
          notificationsOpen = false,
          currentScreen = CareLoopScreen.Doctor(docId, msg.targetDoctorTab),
          focusedDoctorPatientId = msg.targetPatientId,
          auditLogs = logs,
          auditIntegrityResult = if (!msg.isRead) null else state.auditIntegrityResult,
        )
      } else {
        state.copy(
          inboxMessages = updatedMessages,
          notificationsOpen = false,
          auditLogs = logs,
          auditIntegrityResult = if (!msg.isRead) null else state.auditIntegrityResult,
        )
      }
    }
  }

  fun markAllNotificationsRead() {
    updateAndPersist { state ->
      val user = state.loggedInUser ?: return@updateAndPersist state
      val updatedMessages = state.inboxMessages.map { msg ->
        if (isMessageForUser(msg, user) && !msg.isRead) msg.copy(isRead = true) else msg
      }
      state.copy(inboxMessages = updatedMessages)
    }
  }

  fun sendReminder(patientId: Int) {
    updateAndPersist { state ->
      val patient = state.patients.find { it.id == patientId } ?: return@updateAndPersist state
      if (!patient.hasConsentForPreferredMethod()) {
        val feedback = ReminderFeedback(
          type = ReminderFeedbackType.BLOCKED,
          message = "Blocked: no consent.",
        )
        val logs = appendAuditEntry(
          existingLogs = state.auditLogs,
          who = state.loggedInUser?.displayName ?: "Clinic Desk",
          role = "Receptionist",
          action = "Reminder blocked (${patient.preferredContactMethod.label}) for ${patient.name}",
          outcome = AuditOutcome.DENIED,
        )
        val adminAlert = createAdminAlert(
          title = "Reminder blocked",
          text = "Reminder for ${patient.name} was blocked because reminder consent is off.",
          category = NotificationCategory.CONSENT_BLOCKED,
          targetPatientId = patient.id,
        )
        return@updateAndPersist state.copy(
          reminderFeedbackByPatientId = state.reminderFeedbackByPatientId + (patientId to feedback),
          inboxMessages = listOf(adminAlert) + state.inboxMessages,
          topToastMessage = notifyIfForCurrentUser(state, adminAlert),
          auditLogs = logs,
          auditIntegrityResult = null,
        )
      }

      if (!patient.hasContactForPreferredMethod()) {
        val feedback = ReminderFeedback(
          type = ReminderFeedbackType.FAILED,
          message = "Failed: missing contact detail.",
        )
        val logs = appendAuditEntry(
          existingLogs = state.auditLogs,
          who = state.loggedInUser?.displayName ?: "Clinic Desk",
          role = "Receptionist",
          action = "Reminder failed (${patient.preferredContactMethod.label}) for ${patient.name}: missing contact detail",
          outcome = AuditOutcome.DENIED,
        )
        val adminAlert = createAdminAlert(
          title = "Delivery failed",
          text = "Reminder for ${patient.name} failed to deliver due to missing contact details.",
          category = NotificationCategory.DELIVERY_FAILED,
          targetPatientId = patient.id,
        )
        return@updateAndPersist state.copy(
          reminderFeedbackByPatientId = state.reminderFeedbackByPatientId + (patientId to feedback),
          reminderStatusByPatientId = state.reminderStatusByPatientId + (patientId to ReminderDeliveryStatus.FAILED),
          inboxMessages = listOf(adminAlert) + state.inboxMessages,
          topToastMessage = notifyIfForCurrentUser(state, adminAlert),
          auditLogs = logs,
          auditIntegrityResult = null,
        )
      }

      val feedback = ReminderFeedback(
        type = ReminderFeedbackType.SENT,
        message = "Reminder sent (simulated).",
      )
      val newMessage = InboxMessage(
        id = nextMessageId++,
        patientId = patient.id,
        recipientKey = DemoClinicData.patientAccountKey(patient.id),
        title = "Appointment Reminder",
        channelLabel = patient.preferredContactMethod.label,
        timestampLabel = currentDateTimeLabel(),
        text = "Hello ${patient.firstName}, reminder for your appointment with ${patient.assignedDoctor.displayName} (${patient.appointmentTimeLabel}).",
        isRead = false,
        isReminder = true,
      )
      var logs = appendAuditEntry(
        existingLogs = state.auditLogs,
        who = state.loggedInUser?.displayName ?: "Clinic Desk",
        role = "Receptionist",
        action = "Reminder sent (${patient.preferredContactMethod.label}) to ${patient.name}",
        outcome = AuditOutcome.ALLOWED,
      )
      logs = appendAuditEntry(
        existingLogs = logs,
        who = "CareLoop System",
        role = "System",
        action = "Reminder delivered (${patient.preferredContactMethod.label}) to ${patient.name}",
        outcome = AuditOutcome.ALLOWED,
      )
      state.copy(
        reminderFeedbackByPatientId = state.reminderFeedbackByPatientId + (patientId to feedback),
        reminderStatusByPatientId = state.reminderStatusByPatientId + (patientId to ReminderDeliveryStatus.DELIVERED),
        inboxMessages = listOf(newMessage) + state.inboxMessages,
        topToastMessage = notifyIfForCurrentUser(state, newMessage),
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun draftReminderWithAi(patientId: Int) {
    val currentState = _uiState.value
    val patient = currentState.patients.find { it.id == patientId } ?: return
    if (patientId in currentState.aiDraftLoadingPatientIds) return

    // Check consent BEFORE calling Gemini
    if (!patient.hasConsentForPreferredMethod()) {
      updateAndPersist { state ->
        val logs = appendAuditEntry(
          existingLogs = state.auditLogs,
          who = state.loggedInUser?.displayName ?: "Clinic Desk",
          role = state.loggedInUser?.roleLabel ?: "Receptionist",
          action = "AI reminder draft blocked (${patient.preferredContactMethod.label}) for ${patient.name} (no consent)",
          outcome = AuditOutcome.DENIED,
        )
        val adminAlert = createAdminAlert(
          title = "Reminder blocked",
          text = "AI reminder draft for ${patient.name} was blocked because reminder consent is off.",
          category = NotificationCategory.CONSENT_BLOCKED,
          targetPatientId = patient.id,
        )
        state.copy(
          reminderFeedbackByPatientId = state.reminderFeedbackByPatientId + (
            patientId to ReminderFeedback(
              type = ReminderFeedbackType.BLOCKED,
              message = "Blocked: no consent",
            )
          ),
          aiReminderDraftsByPatientId = state.aiReminderDraftsByPatientId - patientId,
          inboxMessages = listOf(adminAlert) + state.inboxMessages,
          topToastMessage = notifyIfForCurrentUser(state, adminAlert),
          auditLogs = logs,
          auditIntegrityResult = null,
        )
      }
      return
    }

    // Send ONLY first name, appointment date/time, doctor name, and preferred channel.
    val draftRequest = ReminderDraftRequest(
      firstName = patient.firstName,
      appointmentDateTime = patient.appointmentTimeLabel,
      doctorName = patient.assignedDoctor.displayName,
      preferredChannel = patient.preferredContactMethod.label,
    )

    _uiState.update { state ->
      state.copy(
        aiDraftLoadingPatientIds = state.aiDraftLoadingPatientIds + patientId,
        reminderFeedbackByPatientId = state.reminderFeedbackByPatientId - patientId,
      )
    }

    viewModelScope.launch {
      val response = aiGateway.draftReminder(draftRequest)
      updateAndPersist { state ->
        val latestPatient = state.patients.find { it.id == patientId } ?: patient
        val draft = AiReminderDraft(
          patientId = patientId,
          text = response.text,
          isEditing = false,
          usedFallback = response.usedFallback,
          statusNotice = if (response.usedFallback) "AI unavailable, using standard message" else null,
        )
        val logs = appendAuditEntry(
          existingLogs = state.auditLogs,
          who = state.loggedInUser?.displayName ?: "Clinic Desk",
          role = state.loggedInUser?.roleLabel ?: "Receptionist",
          action = if (response.usedFallback) {
            "AI reminder draft created using standard fallback (${latestPatient.preferredContactMethod.label}) for ${latestPatient.name}"
          } else {
            "AI reminder draft created (${latestPatient.preferredContactMethod.label}) for ${latestPatient.name}"
          },
          outcome = AuditOutcome.ALLOWED,
        )
        state.copy(
          aiDraftLoadingPatientIds = state.aiDraftLoadingPatientIds - patientId,
          aiReminderDraftsByPatientId = state.aiReminderDraftsByPatientId + (patientId to draft),
          auditLogs = logs,
          auditIntegrityResult = null,
        )
      }
    }
  }

  fun startEditingAiDraft(patientId: Int) {
    _uiState.update { state ->
      val draft = state.aiReminderDraftsByPatientId[patientId] ?: return@update state
      state.copy(
        aiReminderDraftsByPatientId = state.aiReminderDraftsByPatientId + (
          patientId to draft.copy(isEditing = true)
        ),
      )
    }
  }

  fun updateAiDraftText(patientId: Int, newText: String) {
    _uiState.update { state ->
      val draft = state.aiReminderDraftsByPatientId[patientId] ?: return@update state
      state.copy(
        aiReminderDraftsByPatientId = state.aiReminderDraftsByPatientId + (
          patientId to draft.copy(text = newText, wasEdited = true, editLogged = false)
        ),
      )
    }
  }

  fun finishEditingAiDraft(patientId: Int) {
    updateAndPersist { state ->
      val draft = state.aiReminderDraftsByPatientId[patientId] ?: return@updateAndPersist state
      val patient = state.patients.find { it.id == patientId } ?: return@updateAndPersist state
      val shouldLogEdit = draft.wasEdited && !draft.editLogged
      val logs = if (shouldLogEdit) {
        appendAuditEntry(
          existingLogs = state.auditLogs,
          who = state.loggedInUser?.displayName ?: "Clinic Desk",
          role = state.loggedInUser?.roleLabel ?: "Receptionist",
          action = "AI reminder draft edited for ${patient.name}",
          outcome = AuditOutcome.ALLOWED,
        )
      } else {
        state.auditLogs
      }
      state.copy(
        aiReminderDraftsByPatientId = state.aiReminderDraftsByPatientId + (
          patientId to draft.copy(isEditing = false, editLogged = draft.editLogged || shouldLogEdit)
        ),
        auditLogs = logs,
        auditIntegrityResult = if (shouldLogEdit) null else state.auditIntegrityResult,
      )
    }
  }

  fun approveAndSendAiDraft(patientId: Int) {
    updateAndPersist { state ->
      val draft = state.aiReminderDraftsByPatientId[patientId] ?: return@updateAndPersist state
      val patient = state.patients.find { it.id == patientId } ?: return@updateAndPersist state
      val cleanText = draft.text.trim()
      if (cleanText.isEmpty()) return@updateAndPersist state

      if (!patient.hasConsentForPreferredMethod()) {
        val logs = appendAuditEntry(
          existingLogs = state.auditLogs,
          who = state.loggedInUser?.displayName ?: "Clinic Desk",
          role = state.loggedInUser?.roleLabel ?: "Receptionist",
          action = "AI reminder send blocked (${patient.preferredContactMethod.label}) for ${patient.name} (no consent)",
          outcome = AuditOutcome.DENIED,
        )
        val adminAlert = createAdminAlert(
          title = "Reminder blocked",
          text = "AI reminder for ${patient.name} was blocked because reminder consent is off.",
          category = NotificationCategory.CONSENT_BLOCKED,
          targetPatientId = patient.id,
        )
        return@updateAndPersist state.copy(
          aiReminderDraftsByPatientId = state.aiReminderDraftsByPatientId - patientId,
          reminderFeedbackByPatientId = state.reminderFeedbackByPatientId + (
            patientId to ReminderFeedback(
              type = ReminderFeedbackType.BLOCKED,
              message = "Blocked: no consent",
            )
          ),
          inboxMessages = listOf(adminAlert) + state.inboxMessages,
          topToastMessage = notifyIfForCurrentUser(state, adminAlert),
          auditLogs = logs,
          auditIntegrityResult = null,
        )
      }

      if (!patient.hasContactForPreferredMethod()) {
        val logs = appendAuditEntry(
          existingLogs = state.auditLogs,
          who = state.loggedInUser?.displayName ?: "Clinic Desk",
          role = state.loggedInUser?.roleLabel ?: "Receptionist",
          action = "AI reminder send failed (${patient.preferredContactMethod.label}) for ${patient.name}: missing contact detail",
          outcome = AuditOutcome.DENIED,
        )
        val adminAlert = createAdminAlert(
          title = "Delivery failed",
          text = "AI reminder for ${patient.name} failed to deliver due to missing contact details.",
          category = NotificationCategory.DELIVERY_FAILED,
          targetPatientId = patient.id,
        )
        return@updateAndPersist state.copy(
          aiReminderDraftsByPatientId = state.aiReminderDraftsByPatientId - patientId,
          reminderStatusByPatientId = state.reminderStatusByPatientId + (patientId to ReminderDeliveryStatus.FAILED),
          reminderFeedbackByPatientId = state.reminderFeedbackByPatientId + (
            patientId to ReminderFeedback(
              type = ReminderFeedbackType.FAILED,
              message = "Failed: missing contact detail.",
            )
          ),
          inboxMessages = listOf(adminAlert) + state.inboxMessages,
          topToastMessage = notifyIfForCurrentUser(state, adminAlert),
          auditLogs = logs,
          auditIntegrityResult = null,
        )
      }

      var logs = state.auditLogs
      if (draft.wasEdited && !draft.editLogged) {
        logs = appendAuditEntry(
          existingLogs = logs,
          who = state.loggedInUser?.displayName ?: "Clinic Desk",
          role = state.loggedInUser?.roleLabel ?: "Receptionist",
          action = "AI reminder draft edited for ${patient.name}",
          outcome = AuditOutcome.ALLOWED,
        )
      }
      logs = appendAuditEntry(
        existingLogs = logs,
        who = state.loggedInUser?.displayName ?: "Clinic Desk",
        role = state.loggedInUser?.roleLabel ?: "Receptionist",
        action = "AI reminder draft approved and sent (${patient.preferredContactMethod.label}) to ${patient.name}",
        outcome = AuditOutcome.ALLOWED,
      )
      logs = appendAuditEntry(
        existingLogs = logs,
        who = "CareLoop System",
        role = "System",
        action = "Reminder delivered (${patient.preferredContactMethod.label}) to ${patient.name}",
        outcome = AuditOutcome.ALLOWED,
      )

      val newMessage = InboxMessage(
        id = nextMessageId++,
        patientId = patient.id,
        recipientKey = DemoClinicData.patientAccountKey(patient.id),
        title = "Appointment Reminder",
        channelLabel = patient.preferredContactMethod.label,
        timestampLabel = currentDateTimeLabel(),
        text = cleanText,
        isRead = false,
        isReminder = true,
      )
      state.copy(
        aiReminderDraftsByPatientId = state.aiReminderDraftsByPatientId - patientId,
        reminderStatusByPatientId = state.reminderStatusByPatientId + (patientId to ReminderDeliveryStatus.DELIVERED),
        reminderFeedbackByPatientId = state.reminderFeedbackByPatientId + (
          patientId to ReminderFeedback(
            type = ReminderFeedbackType.SENT,
            message = "AI reminder approved and sent (simulated).",
          )
        ),
        inboxMessages = listOf(newMessage) + state.inboxMessages,
        topToastMessage = notifyIfForCurrentUser(state, newMessage),
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun discardAiDraft(patientId: Int) {
    updateAndPersist { state ->
      if (patientId !in state.aiReminderDraftsByPatientId) return@updateAndPersist state
      val patient = state.patients.find { it.id == patientId } ?: return@updateAndPersist state
      val logs = appendAuditEntry(
        existingLogs = state.auditLogs,
        who = state.loggedInUser?.displayName ?: "Clinic Desk",
        role = state.loggedInUser?.roleLabel ?: "Receptionist",
        action = "AI reminder draft discarded for ${patient.name}",
        outcome = AuditOutcome.ALLOWED,
      )
      state.copy(
        aiReminderDraftsByPatientId = state.aiReminderDraftsByPatientId - patientId,
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun refreshDoctorDailySummary(doctorId: DoctorId) {
    val currentSummary = _uiState.value.doctorSummaryByDoctorId[doctorId]
    if (currentSummary?.isLoading == true) return

    val topPatients = _uiState.value.patients
      .filter { it.assignedDoctor == doctorId && !it.cancelled && it.attendanceOutcome != AttendanceOutcome.ATTENDED }
      .sortedWith(compareByDescending<Patient> { it.riskScore() }.thenBy { it.appointmentHoursFromNow })
      .ifEmpty {
        _uiState.value.patients
          .filter { it.assignedDoctor == doctorId }
          .sortedWith(compareByDescending<Patient> { it.riskScore() }.thenBy { it.appointmentHoursFromNow })
      }
      .take(3)
      .map { patient ->
        HighRiskSummaryPatient(
          firstName = patient.firstName,
          appointmentDateTime = patient.appointmentTimeLabel,
          riskReason = patient.riskReasonSummary(),
        )
      }

    _uiState.update { state ->
      val previousText = state.doctorSummaryByDoctorId[doctorId]?.text.orEmpty()
      state.copy(
        doctorSummaryByDoctorId = state.doctorSummaryByDoctorId + (
          doctorId to DoctorDailySummary(
            doctorId = doctorId,
            text = previousText,
            isLoading = true,
          )
        ),
      )
    }

    viewModelScope.launch {
      val response = aiGateway.summarizeDoctorTopRisk(doctorId.displayName, topPatients)
      updateAndPersist { state ->
        val logs = appendAuditEntry(
          existingLogs = state.auditLogs,
          who = state.loggedInUser?.displayName ?: doctorId.displayName,
          role = state.loggedInUser?.roleLabel ?: doctorId.specializationLabel,
          action = if (response.usedFallback) {
            "Daily summary generated from data for ${doctorId.shortTitle}"
          } else {
            "AI daily summary generated for ${doctorId.shortTitle}"
          },
          outcome = AuditOutcome.ALLOWED,
        )
        state.copy(
          doctorSummaryByDoctorId = state.doctorSummaryByDoctorId + (
            doctorId to DoctorDailySummary(
              doctorId = doctorId,
              text = response.text,
              isLoading = false,
              usedFallback = response.usedFallback,
              statusNotice = if (response.usedFallback) "AI unavailable, showing data summary" else null,
            )
          ),
          auditLogs = logs,
          auditIntegrityResult = null,
        )
      }
    }
  }

  fun askDoctorAiAssistant(doctorId: DoctorId, rawQuestion: String) {
    val question = rawQuestion.trim()
    if (question.isEmpty()) return
    if (doctorId in _uiState.value.doctorChatLoadingDoctorIds) return

    val doctorPatients = _uiState.value.patients.filter { it.assignedDoctor == doctorId }
    val context = DoctorAssistantContext(
      doctorName = doctorId.displayName,
      specialization = doctorId.specializationLabel,
      totalAppointments = doctorPatients.count { !it.cancelled },
      urgentCount = doctorPatients.count { it.isUrgent && it.doctorDecision == null },
      totalMissedCount = doctorPatients.sumOf { it.pastMissedAppointments },
      patients = doctorPatients.map { p ->
        // Strictly send ONLY first name, appointment time, and risk reason — NEVER medical notes.
        HighRiskSummaryPatient(
          firstName = p.firstName,
          appointmentDateTime = p.appointmentTimeLabel,
          riskReason = p.riskReasonSummary(),
        )
      },
    )

    val userMsg = DoctorChatMessage(
      id = nextChatMessageId++,
      isUser = true,
      text = question,
      timestampLabel = currentRecordedTimeLabel(),
    )

    _uiState.update { state ->
      val currentHistory = state.doctorChatMessagesByDoctorId[doctorId].orEmpty()
      state.copy(
        doctorChatLoadingDoctorIds = state.doctorChatLoadingDoctorIds + doctorId,
        doctorChatMessagesByDoctorId = state.doctorChatMessagesByDoctorId + (
          doctorId to (currentHistory + userMsg)
        ),
      )
    }

    viewModelScope.launch {
      val response = aiGateway.askDoctorAssistant(context, question)
      updateAndPersist { state ->
        val currentHistory = state.doctorChatMessagesByDoctorId[doctorId].orEmpty()
        val latestDoctorPatients = state.patients.filter { it.assignedDoctor == doctorId }
        val replyMsg = if (response.usedFallback) {
          val reason = response.errorReason ?: FirebaseCareLoopAiGateway.REASON_NO_RESPONSE
          val backupText = ClinicBackupAssistant.answer(
            doctorId = doctorId,
            doctorPatients = latestDoctorPatients,
            bookedAppointments = state.bookedAppointments,
            followUpAppointments = state.followUpAppointments,
            question = question,
          )
          DoctorChatMessage(
            id = nextChatMessageId++,
            isUser = false,
            text = backupText,
            timestampLabel = currentRecordedTimeLabel(),
            isError = false,
            failureReason = reason,
            sourceLabel = "Answered from clinic data.",
          )
        } else {
          DoctorChatMessage(
            id = nextChatMessageId++,
            isUser = false,
            text = response.text,
            timestampLabel = currentRecordedTimeLabel(),
            isError = false,
          )
        }
        // Audit ONLY that AI chat was used, NEVER the chat content
        val logs = appendAuditEntry(
          existingLogs = state.auditLogs,
          who = state.loggedInUser?.displayName ?: doctorId.displayName,
          role = doctorId.specializationLabel,
          action = "AI assistant chat used by ${doctorId.displayName}",
          outcome = AuditOutcome.ALLOWED,
        )
        state.copy(
          doctorChatLoadingDoctorIds = state.doctorChatLoadingDoctorIds - doctorId,
          doctorChatMessagesByDoctorId = state.doctorChatMessagesByDoctorId + (
            doctorId to (currentHistory + replyMsg)
          ),
          auditLogs = logs,
          auditIntegrityResult = null,
        )
      }
    }
  }

  fun submitAppointmentRequest(
    patientId: Int,
    preferredDateKey: String,
    preferredTimeOfDay: TimeOfDay,
    reason: String,
    doctorId: DoctorId? = null,
    preferredSlotIndex: Int? = null,
    privateNote: String? = null,
    fromFollowUpId: Long? = null,
  ) {
    val cleanReason = reason.trim()
    if (preferredDateKey.isBlank() || cleanReason.isEmpty()) return
    if (ClinicSchedule.isPastDateKey(preferredDateKey) || ClinicSchedule.isSundayDateKey(preferredDateKey)) {
      _uiState.update {
        it.copy(schedulingError = "Clinic is closed on Sundays and past dates are not allowed.")
      }
      return
    }

    updateAndPersist { state ->
      val patient = state.patients.find { it.id == patientId } ?: return@updateAndPersist state
      val targetDoctor = doctorId ?: patient.assignedDoctor
      val hasPending = state.appointmentRequests.any {
        it.patientId == patientId && it.status == AppointmentRequestStatus.PENDING
      }
      if (hasPending) {
        val logs = appendAuditEntry(
          existingLogs = state.auditLogs,
          who = patient.name,
          role = "Patient",
          action = "Blocked duplicate appointment request (pending request exists)",
          outcome = AuditOutcome.DENIED,
        )
        return@updateAndPersist state.copy(
          schedulingError = "You already have a pending request.",
          auditLogs = logs,
          auditIntegrityResult = null,
        )
      }

      if (preferredSlotIndex != null && isSlotTaken(state, targetDoctor, preferredDateKey, preferredSlotIndex)) {
        val logs = appendAuditEntry(
          existingLogs = state.auditLogs,
          who = patient.name,
          role = "Patient",
          action = "Blocked request for taken slot with ${targetDoctor.displayName} (${ClinicSchedule.formatDateAndSlot(preferredDateKey, preferredSlotIndex)})",
          outcome = AuditOutcome.DENIED,
        )
        val adminAlert = createAdminAlert(
          title = "Double-booking blocked",
          text = "A double-booking attempt with ${targetDoctor.displayName} was blocked.",
          category = NotificationCategory.DOUBLE_BOOKING_BLOCKED,
          targetPatientId = patient.id,
        )
        return@updateAndPersist state.copy(
          schedulingError = "This slot is already booked.",
          inboxMessages = listOf(adminAlert) + state.inboxMessages,
          auditLogs = logs,
          auditIntegrityResult = null,
        )
      }

      val cleanNote = privateNote?.trim().orEmpty()
      val encryptedNote = if (cleanNote.isNotEmpty()) crypto.encrypt(cleanNote) else null
      val dateLabel = ClinicSchedule.formatDateLabel(preferredDateKey)
      val slotTimeOfDay = preferredSlotIndex?.let { idx ->
        ClinicSchedule.ALL_SLOTS.getOrNull(idx)?.timeOfDay
      } ?: preferredTimeOfDay

      val request = AppointmentRequest(
        id = nextRequestId++,
        patientId = patient.id,
        patientName = patient.name,
        doctorId = targetDoctor,
        preferredDateKey = preferredDateKey,
        preferredDateLabel = dateLabel,
        preferredTimeOfDay = slotTimeOfDay,
        preferredSlotIndex = preferredSlotIndex,
        reason = cleanReason,
        encryptedPrivateNote = encryptedNote,
        status = AppointmentRequestStatus.PENDING,
      )

      val updatedDocFollowUps = if (fromFollowUpId != null) {
        state.doctorRecommendedFollowUps.map {
          if (it.id == fromFollowUpId) it.copy(booked = true) else it
        }
      } else {
        state.doctorRecommendedFollowUps
      }

      val receptionistNotice = InboxMessage(
        id = nextMessageId++,
        patientId = patient.id,
        recipientKey = "receptionist",
        title = "New Appointment Request",
        channelLabel = "Clinic Inbox",
        timestampLabel = currentDateTimeLabel(),
        text = "${patient.name} requested ${targetDoctor.nameWithSpecialization} on $dateLabel (${preferredSlotIndex?.let { ClinicSchedule.slotLabel(it) } ?: slotTimeOfDay.label}).",
        isRead = false,
      )

      val logs = appendAuditEntry(
        existingLogs = state.auditLogs,
        who = patient.name,
        role = "Patient",
        action = "Appointment requested with ${targetDoctor.nameWithSpecialization} ($dateLabel, ${preferredSlotIndex?.let { ClinicSchedule.slotLabel(it) } ?: slotTimeOfDay.label})",
        outcome = AuditOutcome.ALLOWED,
      )
      state.copy(
        appointmentRequests = listOf(request) + state.appointmentRequests,
        doctorRecommendedFollowUps = updatedDocFollowUps,
        inboxMessages = listOf(receptionistNotice) + state.inboxMessages,
        requestSentPatientIds = state.requestSentPatientIds + patientId,
        schedulingError = null,
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun bookAppointmentRequestSlot(
    requestId: Long,
    dateKey: String,
    slotIndex: Int,
  ) {
    updateAndPersist { state ->
      val req = state.appointmentRequests.find { it.id == requestId } ?: return@updateAndPersist state
      if (req.status == AppointmentRequestStatus.BOOKED || req.status == AppointmentRequestStatus.DECLINED) {
        return@updateAndPersist state
      }
      val patient = state.patients.find { it.id == req.patientId } ?: return@updateAndPersist state

      if (isSlotTaken(state, req.doctorId, dateKey, slotIndex)) {
        val logs = appendAuditEntry(
          existingLogs = state.auditLogs,
          who = state.loggedInUser?.displayName ?: "Clinic Desk",
          role = "Receptionist",
          action = "Blocked double-booking attempt for ${req.doctorId.displayName} (${ClinicSchedule.formatDateAndSlot(dateKey, slotIndex)})",
          outcome = AuditOutcome.DENIED,
        )
        val adminAlert = createAdminAlert(
          title = "Double-booking blocked",
          text = "A double-booking attempt for ${req.doctorId.displayName} was blocked.",
          category = NotificationCategory.DOUBLE_BOOKING_BLOCKED,
          targetPatientId = patient.id,
        )
        return@updateAndPersist state.copy(
          schedulingError = "This slot is already booked.",
          inboxMessages = listOf(adminAlert) + state.inboxMessages,
          auditLogs = logs,
          auditIntegrityResult = null,
        )
      }

      val dateLabel = ClinicSchedule.formatDateLabel(dateKey)
      val slotLabel = ClinicSchedule.slotLabel(slotIndex)
      val formattedTime = "$dateLabel · $slotLabel"

      val updatedRequests = state.appointmentRequests.map {
        if (it.id == requestId) {
          it.copy(
            status = AppointmentRequestStatus.BOOKED,
            bookedDateLabel = dateLabel,
            bookedTimeLabel = slotLabel,
          )
        } else {
          it
        }
      }
      val booked = BookedAppointment(
        id = nextBookedId++,
        patientId = patient.id,
        doctorId = req.doctorId,
        dateKey = dateKey,
        slotIndex = slotIndex,
        timeLabel = formattedTime,
        reason = req.reason,
      )
      // Replies to patient's own request always arrive in-app
      val inboxMsg = InboxMessage(
        id = nextMessageId++,
        patientId = patient.id,
        recipientKey = DemoClinicData.patientAccountKey(patient.id),
        title = "Appointment Booked",
        channelLabel = "Clinic Inbox",
        timestampLabel = currentDateTimeLabel(),
        text = "Hello ${patient.firstName}, your appointment with ${req.doctorId.nameWithSpecialization} has been booked for $dateLabel at $slotLabel.",
        isRead = false,
      )
      val doctorAlert = createDoctorAlert(
        doctorId = req.doctorId,
        targetPatientId = patient.id,
        title = "New appointment booked",
        text = "New appointment booked with ${patient.firstName} for $dateLabel at $slotLabel.",
        category = NotificationCategory.NEW_BOOKING,
        targetTab = DoctorTab.TODAY,
      )
      val logs = appendAuditEntry(
        existingLogs = state.auditLogs,
        who = state.loggedInUser?.displayName ?: "Clinic Desk",
        role = "Receptionist",
        action = "Booked appointment for ${patient.name} with ${req.doctorId.displayName} ($formattedTime)",
        outcome = AuditOutcome.ALLOWED,
      )
      state.copy(
        appointmentRequests = updatedRequests,
        bookedAppointments = listOf(booked) + state.bookedAppointments,
        inboxMessages = listOf(inboxMsg, doctorAlert) + state.inboxMessages,
        topToastMessage = notifyAnyForCurrentUser(state, listOf(inboxMsg, doctorAlert)),
        schedulingError = null,
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun offerAnotherTime(
    requestId: Long,
    offeredDateKey: String,
    offeredSlotIndex: Int,
  ) {
    updateAndPersist { state ->
      val req = state.appointmentRequests.find { it.id == requestId } ?: return@updateAndPersist state
      if (req.status == AppointmentRequestStatus.BOOKED || req.status == AppointmentRequestStatus.DECLINED) {
        return@updateAndPersist state
      }
      val patient = state.patients.find { it.id == req.patientId } ?: return@updateAndPersist state

      if (isSlotTaken(state, req.doctorId, offeredDateKey, offeredSlotIndex)) {
        val logs = appendAuditEntry(
          existingLogs = state.auditLogs,
          who = state.loggedInUser?.displayName ?: "Clinic Desk",
          role = "Receptionist",
          action = "Blocked offer for taken slot with ${req.doctorId.displayName} (${ClinicSchedule.formatDateAndSlot(offeredDateKey, offeredSlotIndex)})",
          outcome = AuditOutcome.DENIED,
        )
        val adminAlert = createAdminAlert(
          title = "Double-booking blocked",
          text = "A double-booking offer with ${req.doctorId.displayName} was blocked.",
          category = NotificationCategory.DOUBLE_BOOKING_BLOCKED,
          targetPatientId = patient.id,
        )
        return@updateAndPersist state.copy(
          schedulingError = "This slot is already booked.",
          inboxMessages = listOf(adminAlert) + state.inboxMessages,
          auditLogs = logs,
          auditIntegrityResult = null,
        )
      }

      val dateLabel = ClinicSchedule.formatDateLabel(offeredDateKey)
      val slotLabel = ClinicSchedule.slotLabel(offeredSlotIndex)

      val updatedRequests = state.appointmentRequests.map {
        if (it.id == requestId) {
          it.copy(
            status = AppointmentRequestStatus.NEW_TIME_OFFERED,
            offeredDateKey = offeredDateKey,
            offeredDateLabel = dateLabel,
            offeredSlotIndex = offeredSlotIndex,
            offeredTimeLabel = slotLabel,
          )
        } else {
          it
        }
      }

      val inboxMsg = InboxMessage(
        id = nextMessageId++,
        patientId = patient.id,
        recipientKey = DemoClinicData.patientAccountKey(patient.id),
        title = "New Time Offered",
        channelLabel = "Clinic Inbox",
        timestampLabel = currentDateTimeLabel(),
        text = "Hello ${patient.firstName}, ${req.doctorId.nameWithSpecialization} has an opening on $dateLabel at $slotLabel. Please accept or decline in Appointments.",
        isRead = false,
      )
      val logs = appendAuditEntry(
        existingLogs = state.auditLogs,
        who = state.loggedInUser?.displayName ?: "Clinic Desk",
        role = "Receptionist",
        action = "Offered new time to ${patient.name} with ${req.doctorId.displayName} ($dateLabel · $slotLabel)",
        outcome = AuditOutcome.ALLOWED,
      )
      state.copy(
        appointmentRequests = updatedRequests,
        inboxMessages = listOf(inboxMsg) + state.inboxMessages,
        topToastMessage = notifyIfForCurrentUser(state, inboxMsg),
        schedulingError = null,
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun declineAppointmentRequest(requestId: Long, reason: String) {
    updateAndPersist { state ->
      val req = state.appointmentRequests.find { it.id == requestId } ?: return@updateAndPersist state
      if (req.status == AppointmentRequestStatus.BOOKED || req.status == AppointmentRequestStatus.DECLINED) {
        return@updateAndPersist state
      }
      val patient = state.patients.find { it.id == req.patientId } ?: return@updateAndPersist state

      val updatedRequests = state.appointmentRequests.map {
        if (it.id == requestId) {
          it.copy(
            status = AppointmentRequestStatus.DECLINED,
            declineReason = reason,
          )
        } else {
          it
        }
      }

      val inboxMsg = InboxMessage(
        id = nextMessageId++,
        patientId = patient.id,
        recipientKey = DemoClinicData.patientAccountKey(patient.id),
        title = "Appointment Request Update",
        channelLabel = "Clinic Inbox",
        timestampLabel = currentDateTimeLabel(),
        text = "Hello ${patient.firstName}, your appointment request for ${req.preferredDateLabel} (${req.preferredTimeOfDay.label}) was declined. Reason: $reason.",
        isRead = false,
      )
      val logs = appendAuditEntry(
        existingLogs = state.auditLogs,
        who = state.loggedInUser?.displayName ?: "Clinic Desk",
        role = "Receptionist",
        action = "Declined appointment request for ${patient.name} ($reason)",
        outcome = AuditOutcome.ALLOWED,
      )
      state.copy(
        appointmentRequests = updatedRequests,
        inboxMessages = listOf(inboxMsg) + state.inboxMessages,
        topToastMessage = notifyIfForCurrentUser(state, inboxMsg),
        schedulingError = null,
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun acceptOfferedTime(requestId: Long) {
    updateAndPersist { state ->
      val req = state.appointmentRequests.find { it.id == requestId } ?: return@updateAndPersist state
      if (req.status != AppointmentRequestStatus.NEW_TIME_OFFERED) return@updateAndPersist state
      val dateKey = req.offeredDateKey ?: return@updateAndPersist state
      val slotIndex = req.offeredSlotIndex ?: return@updateAndPersist state
      val patient = state.patients.find { it.id == req.patientId } ?: return@updateAndPersist state

      if (isSlotTaken(state, req.doctorId, dateKey, slotIndex)) {
        val logs = appendAuditEntry(
          existingLogs = state.auditLogs,
          who = patient.name,
          role = "Patient",
          action = "Blocked double-booking on offered time with ${req.doctorId.displayName} (${ClinicSchedule.formatDateAndSlot(dateKey, slotIndex)})",
          outcome = AuditOutcome.DENIED,
        )
        val adminAlert = createAdminAlert(
          title = "Double-booking blocked",
          text = "A double-booking attempt on an offered slot for ${req.doctorId.displayName} was blocked.",
          category = NotificationCategory.DOUBLE_BOOKING_BLOCKED,
          targetPatientId = patient.id,
        )
        return@updateAndPersist state.copy(
          schedulingError = "This slot is already booked.",
          inboxMessages = listOf(adminAlert) + state.inboxMessages,
          auditLogs = logs,
          auditIntegrityResult = null,
        )
      }

      val dateLabel = req.offeredDateLabel ?: ClinicSchedule.formatDateLabel(dateKey)
      val slotLabel = req.offeredTimeLabel ?: ClinicSchedule.slotLabel(slotIndex)
      val formattedTime = "$dateLabel · $slotLabel"

      val updatedRequests = state.appointmentRequests.map {
        if (it.id == requestId) {
          it.copy(
            status = AppointmentRequestStatus.BOOKED,
            bookedDateLabel = dateLabel,
            bookedTimeLabel = slotLabel,
          )
        } else {
          it
        }
      }
      val booked = BookedAppointment(
        id = nextBookedId++,
        patientId = patient.id,
        doctorId = req.doctorId,
        dateKey = dateKey,
        slotIndex = slotIndex,
        timeLabel = formattedTime,
        reason = req.reason,
      )
      val inboxMsg = InboxMessage(
        id = nextMessageId++,
        patientId = patient.id,
        recipientKey = DemoClinicData.patientAccountKey(patient.id),
        title = "Appointment Confirmed",
        channelLabel = "Clinic Inbox",
        timestampLabel = currentDateTimeLabel(),
        text = "Hello ${patient.firstName}, your appointment with ${req.doctorId.nameWithSpecialization} is confirmed for $dateLabel at $slotLabel.",
        isRead = false,
      )
      val doctorAlert = createDoctorAlert(
        doctorId = req.doctorId,
        targetPatientId = patient.id,
        title = "New time accepted",
        text = "${patient.firstName} accepted the offered $dateLabel · $slotLabel slot.",
        category = NotificationCategory.OFFER_RESPONSE,
        targetTab = DoctorTab.TODAY,
      )
      val logs = appendAuditEntry(
        existingLogs = state.auditLogs,
        who = patient.name,
        role = "Patient",
        action = "Accepted offered appointment with ${req.doctorId.displayName} ($formattedTime)",
        outcome = AuditOutcome.ALLOWED,
      )
      state.copy(
        appointmentRequests = updatedRequests,
        bookedAppointments = listOf(booked) + state.bookedAppointments,
        inboxMessages = listOf(inboxMsg, doctorAlert) + state.inboxMessages,
        topToastMessage = notifyAnyForCurrentUser(state, listOf(inboxMsg, doctorAlert)),
        schedulingError = null,
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun declineOfferedTime(requestId: Long) {
    updateAndPersist { state ->
      val req = state.appointmentRequests.find { it.id == requestId } ?: return@updateAndPersist state
      if (req.status != AppointmentRequestStatus.NEW_TIME_OFFERED) return@updateAndPersist state
      val patient = state.patients.find { it.id == req.patientId } ?: return@updateAndPersist state

      val updatedRequests = state.appointmentRequests.map {
        if (it.id == requestId) {
          it.copy(
            status = AppointmentRequestStatus.DECLINED,
            declineReason = "Offered time declined by patient",
          )
        } else {
          it
        }
      }
      val doctorAlert = createDoctorAlert(
        doctorId = req.doctorId,
        targetPatientId = patient.id,
        title = "New time declined",
        text = "${patient.firstName} declined the offered ${req.offeredDateLabel ?: ""} · ${req.offeredTimeLabel ?: ""} slot.",
        category = NotificationCategory.OFFER_RESPONSE,
        targetTab = DoctorTab.PATIENTS,
      )
      val logs = appendAuditEntry(
        existingLogs = state.auditLogs,
        who = patient.name,
        role = "Patient",
        action = "Declined offered appointment time (${req.offeredDateLabel} · ${req.offeredTimeLabel})",
        outcome = AuditOutcome.ALLOWED,
      )
      state.copy(
        appointmentRequests = updatedRequests,
        inboxMessages = listOf(doctorAlert) + state.inboxMessages,
        topToastMessage = notifyIfForCurrentUser(state, doctorAlert),
        schedulingError = null,
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun rescheduleAppointment(
    patientId: Int,
    newDateKey: String,
    newSlotIndex: Int,
  ) {
    if (ClinicSchedule.isPastDateKey(newDateKey) || ClinicSchedule.isSundayDateKey(newDateKey)) {
      _uiState.update {
        it.copy(schedulingError = "Clinic is closed on Sundays and past dates are not allowed.")
      }
      return
    }
    updateAndPersist { state ->
      val patient = state.patients.find { it.id == patientId } ?: return@updateAndPersist state
      if (isSlotTaken(state, patient.assignedDoctor, newDateKey, newSlotIndex)) {
        val logs = appendAuditEntry(
          existingLogs = state.auditLogs,
          who = patient.name,
          role = "Patient",
          action = "Blocked double-booking on reschedule for ${patient.assignedDoctor.displayName} (${ClinicSchedule.formatDateAndSlot(newDateKey, newSlotIndex)})",
          outcome = AuditOutcome.DENIED,
        )
        val adminAlert = createAdminAlert(
          title = "Double-booking blocked",
          text = "A double-booking reschedule attempt for ${patient.assignedDoctor.displayName} was blocked.",
          category = NotificationCategory.DOUBLE_BOOKING_BLOCKED,
          targetPatientId = patient.id,
        )
        return@updateAndPersist state.copy(
          schedulingError = "This slot is already booked.",
          inboxMessages = listOf(adminAlert) + state.inboxMessages,
          auditLogs = logs,
          auditIntegrityResult = null,
        )
      }

      val formattedTime = ClinicSchedule.formatDateAndSlot(newDateKey, newSlotIndex)
      val updatedPatients = state.patients.map { p ->
        if (p.id == patientId) {
          p.copy(
            appointmentDateKey = newDateKey,
            appointmentSlotIndex = newSlotIndex,
            appointmentTimeLabel = formattedTime,
            confirmed = true,
            cancelled = false,
            rescheduled = true,
            attendanceOutcome = null,
            attendanceRecordedAt = null,
          )
        } else {
          p
        }
      }
      val inboxMsg = InboxMessage(
        id = nextMessageId++,
        patientId = patient.id,
        recipientKey = DemoClinicData.patientAccountKey(patient.id),
        title = "Appointment Rescheduled",
        channelLabel = "Clinic Inbox",
        timestampLabel = currentDateTimeLabel(),
        text = "Hello ${patient.firstName}, your appointment with ${patient.assignedDoctor.nameWithSpecialization} is rescheduled to $formattedTime.",
        isRead = false,
      )
      val logs = appendAuditEntry(
        existingLogs = state.auditLogs,
        who = patient.name,
        role = "Patient",
        action = "Rescheduled appointment with ${patient.assignedDoctor.displayName} ($formattedTime)",
        outcome = AuditOutcome.ALLOWED,
      )
      state.copy(
        patients = updatedPatients,
        inboxMessages = listOf(inboxMsg) + state.inboxMessages,
        topToastMessage = notifyIfForCurrentUser(state, inboxMsg),
        schedulingError = null,
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun setDoctorDecision(doctorId: DoctorId, patientId: Int, decision: DoctorDecision) {
    updateAndPersist { state ->
      val patient = state.patients.find { it.id == patientId } ?: return@updateAndPersist state
      val updatedPatients = state.patients.map {
        if (it.id == patientId) it.copy(doctorDecision = decision) else it
      }
      var logs = appendAuditEntry(
        existingLogs = state.auditLogs,
        who = doctorId.displayName,
        role = doctorId.specializationLabel,
        action = "Doctor decision: ${decision.label} follow-up for ${patient.name}",
        outcome = AuditOutcome.ALLOWED,
      )

      if (decision == DoctorDecision.APPROVED && patient.isUrgent) {
        val alreadyHasFollowUp = state.followUpAppointments.any { it.patientId == patientId }
        if (!alreadyHasFollowUp) {
          val openDates = ClinicSchedule.openDateKeysWithin(startOffsetDays = 1, daysWindow = 7)
          var chosenSlot: Pair<String, Int>? = null
          for (dateKey in openDates) {
            val freeSlot = ClinicSchedule.ALL_SLOTS.firstOrNull { slot ->
              !isSlotTaken(state, doctorId, dateKey, slot.index)
            }
            if (freeSlot != null) {
              chosenSlot = dateKey to freeSlot.index
              break
            }
          }

          if (chosenSlot == null) {
            logs = appendAuditEntry(
              existingLogs = logs,
              who = doctorId.displayName,
              role = doctorId.specializationLabel,
              action = "Blocked follow-up booking for ${patient.name} (no free slots within 7 days)",
              outcome = AuditOutcome.DENIED,
            )
            val adminAlert = createAdminAlert(
              title = "Double-booking blocked",
              text = "Urgent follow-up slot booking for ${doctorId.displayName} was blocked (no open slot).",
              category = NotificationCategory.DOUBLE_BOOKING_BLOCKED,
              targetPatientId = patient.id,
            )
            return@updateAndPersist state.copy(
              patients = updatedPatients,
              schedulingError = "This slot is already booked.",
              inboxMessages = listOf(adminAlert) + state.inboxMessages,
              auditLogs = logs,
              auditIntegrityResult = null,
            )
          }

          val (followUpDateKey, followUpSlotIndex) = chosenSlot
          val followUpTime = ClinicSchedule.formatDateAndSlot(followUpDateKey, followUpSlotIndex)
          val followUp = FollowUpAppointment(
            id = nextFollowUpId++,
            patientId = patient.id,
            doctorId = doctorId,
            dateKey = followUpDateKey,
            slotIndex = followUpSlotIndex,
            timeLabel = followUpTime,
          )
          logs = appendAuditEntry(
            existingLogs = logs,
            who = doctorId.displayName,
            role = doctorId.specializationLabel,
            action = "Follow-up appointment created for ${patient.name} ($followUpTime)",
            outcome = AuditOutcome.ALLOWED,
          )
          val doctorBookingAlert = createDoctorAlert(
            doctorId = doctorId,
            targetPatientId = patient.id,
            title = "Follow-up booked",
            text = "Urgent follow-up booked with ${patient.firstName} ($followUpTime).",
            category = NotificationCategory.NEW_BOOKING,
            targetTab = DoctorTab.TODAY,
          )
          // Never put medical notes in a message; only send notification if consent allows
          if (patient.hasConsentForPreferredMethod()) {
            val inboxMsg = InboxMessage(
              id = nextMessageId++,
              patientId = patient.id,
              recipientKey = DemoClinicData.patientAccountKey(patient.id),
              title = "Follow-up Scheduled",
              channelLabel = patient.preferredContactMethod.label,
              timestampLabel = currentDateTimeLabel(),
              text = "Hello ${patient.firstName}, ${doctorId.nameWithSpecialization} scheduled a follow-up appointment for you ($followUpTime).",
              isRead = false,
            )
            logs = appendAuditEntry(
              existingLogs = logs,
              who = "CareLoop System",
              role = "System",
              action = "Follow-up message delivered (${patient.preferredContactMethod.label}) to ${patient.name}",
              outcome = AuditOutcome.ALLOWED,
            )
            return@updateAndPersist state.copy(
              patients = updatedPatients,
              followUpAppointments = listOf(followUp) + state.followUpAppointments,
              inboxMessages = listOf(inboxMsg, doctorBookingAlert) + state.inboxMessages,
              topToastMessage = notifyIfForCurrentUser(state, inboxMsg),
              schedulingError = null,
              auditLogs = logs,
              auditIntegrityResult = null,
            )
          } else {
            logs = appendAuditEntry(
              existingLogs = logs,
              who = "CareLoop System",
              role = "System",
              action = "Follow-up message blocked (${patient.preferredContactMethod.label}) for ${patient.name}",
              outcome = AuditOutcome.DENIED,
            )
            val adminAlert = createAdminAlert(
              title = "Reminder blocked",
              text = "A follow-up reminder for ${patient.name} was blocked due to no consent.",
              category = NotificationCategory.CONSENT_BLOCKED,
              targetPatientId = patient.id,
            )
            return@updateAndPersist state.copy(
              patients = updatedPatients,
              followUpAppointments = listOf(followUp) + state.followUpAppointments,
              inboxMessages = listOf(doctorBookingAlert, adminAlert) + state.inboxMessages,
              schedulingError = null,
              auditLogs = logs,
              auditIntegrityResult = null,
            )
          }
        }
      }

      state.copy(
        patients = updatedPatients,
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun createDoctorFollowUp(
    doctorId: DoctorId,
    patientId: Int,
    reason: String,
    dueInDays: Int,
  ) {
    val cleanReason = reason.trim()
    if (cleanReason.isEmpty()) return

    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, dueInDays) }
    val dueDateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    val dueDateLabel = SimpleDateFormat("d MMM", Locale.US).format(cal.time)

    updateAndPersist { state ->
      val patient = state.patients.find { it.id == patientId } ?: return@updateAndPersist state
      val item = DoctorRecommendedFollowUp(
        id = nextDoctorFollowUpId++,
        patientId = patient.id,
        doctorId = doctorId,
        reason = cleanReason,
        dueInDays = dueInDays,
        dueDateKey = dueDateKey,
        dueDateLabel = dueDateLabel,
        booked = false,
      )
      val inboxMsg = InboxMessage(
        id = nextMessageId++,
        patientId = patient.id,
        recipientKey = DemoClinicData.patientAccountKey(patient.id),
        title = "Follow-up Recommended",
        channelLabel = "Clinic Inbox",
        timestampLabel = currentDateTimeLabel(),
        text = "Follow-up due by ${doctorId.displayName} (${doctorId.specializationLabel}), $cleanReason, due $dueDateLabel.",
        isRead = false,
      )
      val doctorAlert = createDoctorAlert(
        doctorId = doctorId,
        targetPatientId = patient.id,
        title = "Follow-up booked",
        text = "Follow-up created for ${patient.firstName}, due $dueDateLabel.",
        category = NotificationCategory.NEW_BOOKING,
        targetTab = DoctorTab.PATIENTS,
      )
      val logs = appendAuditEntry(
        existingLogs = state.auditLogs,
        who = doctorId.displayName,
        role = doctorId.specializationLabel,
        action = "Follow-up created for ${patient.name}: $cleanReason (due in $dueInDays days, $dueDateLabel)",
        outcome = AuditOutcome.ALLOWED,
      )
      state.copy(
        doctorRecommendedFollowUps = listOf(item) + state.doctorRecommendedFollowUps,
        inboxMessages = listOf(inboxMsg, doctorAlert) + state.inboxMessages,
        topToastMessage = notifyIfForCurrentUser(state, inboxMsg),
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun recordAttendance(doctorId: DoctorId, patientId: Int, outcome: AttendanceOutcome) {
    updateAndPersist { state ->
      val patient = state.patients.find { it.id == patientId } ?: return@updateAndPersist state
      if (patient.assignedDoctor != doctorId || patient.attendanceOutcome != null) {
        return@updateAndPersist state
      }

      val recordedAt = currentRecordedTimeLabel()
      val missedDateKey = patient.appointmentDateKey.ifBlank { ClinicSchedule.todayLocalDateKey() }
      val missedDateLabel = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date())
      val newMissedId = System.currentTimeMillis() + patient.missedHistory.size + 1L

      val updatedPatients = state.patients.map {
        if (it.id == patientId) {
          when (outcome) {
            AttendanceOutcome.ATTENDED -> it.copy(
              attendanceOutcome = AttendanceOutcome.ATTENDED,
              attendanceRecordedAt = recordedAt,
            )
            AttendanceOutcome.NO_SHOW -> {
              val newRecord = MissedAppointmentRecord(
                id = newMissedId,
                dateKey = missedDateKey,
                dateLabel = missedDateLabel,
                doctorName = doctorId.displayName,
              )
              it.copy(
                attendanceOutcome = AttendanceOutcome.NO_SHOW,
                attendanceRecordedAt = recordedAt,
                pastMissedAppointments = it.pastMissedAppointments + 1,
                missedHistory = (listOf(newRecord) + it.missedHistory)
                  .sortedByDescending { rec -> rec.dateKey },
              )
            }
          }
        } else {
          it
        }
      }

      val logs = appendAuditEntry(
        existingLogs = state.auditLogs,
        who = doctorId.displayName,
        role = doctorId.specializationLabel,
        action = "Recorded ${outcome.label} for ${patient.name} ($recordedAt)",
        outcome = AuditOutcome.ALLOWED,
      )

      val updatedTasks = if (outcome == AttendanceOutcome.NO_SHOW) {
        val task = MissedFollowUpTask(
          id = nextMissedTaskId++,
          patientId = patient.id,
          patientName = patient.name,
          missedDateLabel = ClinicSchedule.formatDateLabel(patient.appointmentDateKey),
        )
        listOf(task) + state.missedFollowUpTasks
      } else {
        state.missedFollowUpTasks
      }

      val doctorMissedAlert = if (outcome == AttendanceOutcome.NO_SHOW) {
        listOf(
          createDoctorAlert(
            doctorId = doctorId,
            targetPatientId = patient.id,
            title = "Missed appointment",
            text = "${patient.firstName} missed their appointment (${ClinicSchedule.formatDateLabel(missedDateKey)}).",
            category = NotificationCategory.MISSED_NO_SHOW,
            targetTab = DoctorTab.PATIENTS,
          ),
        )
      } else {
        emptyList()
      }

      state.copy(
        patients = updatedPatients,
        missedFollowUpTasks = updatedTasks,
        inboxMessages = doctorMissedAlert + state.inboxMessages,
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun sendMissedFollowUpMessage(taskId: Long) {
    updateAndPersist { state ->
      val task = state.missedFollowUpTasks.find { it.id == taskId } ?: return@updateAndPersist state
      if (task.done) return@updateAndPersist state
      val patient = state.patients.find { it.id == task.patientId } ?: return@updateAndPersist state

      if (!patient.hasConsentForPreferredMethod()) {
        val feedback = ReminderFeedback(
          type = ReminderFeedbackType.BLOCKED,
          message = "Blocked: no consent.",
        )
        val updatedTasks = state.missedFollowUpTasks.map {
          if (it.id == taskId) it.copy(done = true, feedback = feedback) else it
        }
        val logs = appendAuditEntry(
          existingLogs = state.auditLogs,
          who = state.loggedInUser?.displayName ?: "Clinic Desk",
          role = "Receptionist",
          action = "Follow-up message blocked (${patient.preferredContactMethod.label}) for ${patient.name}",
          outcome = AuditOutcome.DENIED,
        )
        val adminAlert = createAdminAlert(
          title = "Reminder blocked",
          text = "A follow-up reminder for ${patient.name} was blocked because consent is off.",
          category = NotificationCategory.CONSENT_BLOCKED,
          targetPatientId = patient.id,
        )
        return@updateAndPersist state.copy(
          missedFollowUpTasks = updatedTasks,
          inboxMessages = listOf(adminAlert) + state.inboxMessages,
          auditLogs = logs,
          auditIntegrityResult = null,
        )
      }

      if (!patient.hasContactForPreferredMethod()) {
        val feedback = ReminderFeedback(
          type = ReminderFeedbackType.FAILED,
          message = "Failed: missing contact detail.",
        )
        val updatedTasks = state.missedFollowUpTasks.map {
          if (it.id == taskId) it.copy(done = true, feedback = feedback) else it
        }
        val logs = appendAuditEntry(
          existingLogs = state.auditLogs,
          who = state.loggedInUser?.displayName ?: "Clinic Desk",
          role = "Receptionist",
          action = "Follow-up message failed (${patient.preferredContactMethod.label}) for ${patient.name}: missing contact detail",
          outcome = AuditOutcome.DENIED,
        )
        val adminAlert = createAdminAlert(
          title = "Reminder failed",
          text = "Follow-up delivery failed for ${patient.name} (${patient.preferredContactMethod.label} unavailable).",
          category = NotificationCategory.DELIVERY_FAILED,
          targetPatientId = patient.id,
        )
        return@updateAndPersist state.copy(
          missedFollowUpTasks = updatedTasks,
          reminderStatusByPatientId = state.reminderStatusByPatientId + (patient.id to ReminderDeliveryStatus.FAILED),
          inboxMessages = listOf(adminAlert) + state.inboxMessages,
          auditLogs = logs,
          auditIntegrityResult = null,
        )
      }

      val feedback = ReminderFeedback(
        type = ReminderFeedbackType.SENT,
        message = "Follow-up message sent (simulated).",
      )
      val newMessage = InboxMessage(
        id = nextMessageId++,
        patientId = patient.id,
        recipientKey = DemoClinicData.patientAccountKey(patient.id),
        title = "Missed Appointment Follow-up",
        channelLabel = patient.preferredContactMethod.label,
        timestampLabel = currentDateTimeLabel(),
        text = "Hello ${patient.firstName}, we missed you at your appointment. Would you like to book a new one?",
        isRead = false,
        isReminder = true,
      )
      val updatedTasks = state.missedFollowUpTasks.map {
        if (it.id == taskId) it.copy(done = true, feedback = feedback) else it
      }
      val logs = appendAuditEntry(
        existingLogs = state.auditLogs,
        who = state.loggedInUser?.displayName ?: "Clinic Desk",
        role = "Receptionist",
        action = "Follow-up message sent (${patient.preferredContactMethod.label}) to ${patient.name}",
        outcome = AuditOutcome.ALLOWED,
      )
      state.copy(
        missedFollowUpTasks = updatedTasks,
        reminderStatusByPatientId = state.reminderStatusByPatientId + (patient.id to ReminderDeliveryStatus.DELIVERED),
        inboxMessages = listOf(newMessage) + state.inboxMessages,
        topToastMessage = notifyIfForCurrentUser(state, newMessage),
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun confirmAppointment(patientId: Int) {
    updateAndPersist { state ->
      val patient = state.patients.find { it.id == patientId } ?: return@updateAndPersist state
      val updatedPatients = state.patients.map {
        if (it.id == patientId) it.copy(confirmed = true, cancelled = false) else it
      }
      val doctorAlert = if (!patient.confirmed) {
        listOf(
          createDoctorAlert(
            doctorId = patient.assignedDoctor,
            targetPatientId = patient.id,
            title = "Appointment confirmed",
            text = "${patient.firstName} confirmed their ${patient.appointmentTimeLabel} appointment.",
            category = NotificationCategory.CONFIRMED,
            targetTab = DoctorTab.TODAY,
          ),
        )
      } else {
        emptyList()
      }
      val logs = appendAuditEntry(
        existingLogs = state.auditLogs,
        who = patient.name,
        role = "Patient",
        action = "Appointment confirmed",
        outcome = AuditOutcome.ALLOWED,
      )
      state.copy(
        patients = updatedPatients,
        inboxMessages = doctorAlert + state.inboxMessages,
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun cancelAppointment(patientId: Int) {
    updateAndPersist { state ->
      val patient = state.patients.find { it.id == patientId } ?: return@updateAndPersist state
      val updatedPatients = state.patients.map {
        if (it.id == patientId) it.copy(confirmed = false, cancelled = true) else it
      }
      val doctorAlert = if (!patient.cancelled) {
        listOf(
          createDoctorAlert(
            doctorId = patient.assignedDoctor,
            targetPatientId = patient.id,
            title = "Appointment cancelled",
            text = "${patient.firstName} cancelled their ${patient.appointmentTimeLabel} appointment.",
            category = NotificationCategory.CANCELLED,
            targetTab = DoctorTab.PATIENTS,
          ),
        )
      } else {
        emptyList()
      }
      val logs = appendAuditEntry(
        existingLogs = state.auditLogs,
        who = patient.name,
        role = "Patient",
        action = "Appointment cancelled (slot freed)",
        outcome = AuditOutcome.ALLOWED,
      )
      state.copy(
        patients = updatedPatients,
        inboxMessages = doctorAlert + state.inboxMessages,
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun updatePreferredContactMethod(patientId: Int, method: ContactMethod) {
    updateAndPersist { state ->
      val patient = state.patients.find { it.id == patientId } ?: return@updateAndPersist state
      if (method == ContactMethod.NONE) return@updateAndPersist state
      val allowedByConsent = when (method) {
        ContactMethod.SMS -> patient.smsConsent
        ContactMethod.EMAIL -> patient.emailConsent
        ContactMethod.NONE -> false
      }
      if (!allowedByConsent || patient.preferredContactMethod == method) return@updateAndPersist state

      val updatedPatients = state.patients.map {
        if (it.id == patientId) it.copy(preferredContactMethod = method) else it
      }
      val logs = appendAuditEntry(
        existingLogs = state.auditLogs,
        who = patient.name,
        role = "Patient",
        action = "Reminder preference changed: preferred method set to ${method.label}",
        outcome = AuditOutcome.ALLOWED,
      )
      state.copy(
        patients = updatedPatients,
        reminderFeedbackByPatientId = state.reminderFeedbackByPatientId - patientId,
        preferencesSavedPatientIds = state.preferencesSavedPatientIds + patientId,
        settingsSaved = true,
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun updateSmsConsent(patientId: Int, enabled: Boolean) {
    updateAndPersist { state ->
      val patient = state.patients.find { it.id == patientId } ?: return@updateAndPersist state
      if (patient.smsConsent == enabled) return@updateAndPersist state

      val nextPreferred = when {
        enabled && patient.emailConsent ->
          if (patient.preferredContactMethod == ContactMethod.EMAIL) ContactMethod.EMAIL else ContactMethod.SMS
        enabled && !patient.emailConsent -> ContactMethod.SMS
        !enabled && patient.emailConsent -> ContactMethod.EMAIL
        else -> ContactMethod.NONE
      }

      val updatedPatients = state.patients.map {
        if (it.id == patientId) {
          it.copy(smsConsent = enabled, preferredContactMethod = nextPreferred)
        } else {
          it
        }
      }
      val prefNote = if (nextPreferred != patient.preferredContactMethod) {
        " (preferred method switched to ${nextPreferred.label})"
      } else {
        ""
      }
      val adminAlerts = if (!enabled) {
        listOf(
          createAdminAlert(
            title = "Consent turned off",
            text = "${patient.name} turned off SMS reminder consent.",
            category = NotificationCategory.CONSENT_OFF,
            targetPatientId = patient.id,
          ),
        )
      } else {
        emptyList()
      }
      val logs = appendAuditEntry(
        existingLogs = state.auditLogs,
        who = patient.name,
        role = "Patient",
        action = "Consent changed: SMS ${if (enabled) "On" else "Off"}$prefNote",
        outcome = AuditOutcome.ALLOWED,
      )
      state.copy(
        patients = updatedPatients,
        reminderFeedbackByPatientId = state.reminderFeedbackByPatientId - patientId,
        preferencesSavedPatientIds = state.preferencesSavedPatientIds + patientId,
        settingsSaved = true,
        inboxMessages = adminAlerts + state.inboxMessages,
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun updateEmailConsent(patientId: Int, enabled: Boolean) {
    updateAndPersist { state ->
      val patient = state.patients.find { it.id == patientId } ?: return@updateAndPersist state
      if (patient.emailConsent == enabled) return@updateAndPersist state

      val nextPreferred = when {
        patient.smsConsent && enabled ->
          if (patient.preferredContactMethod == ContactMethod.SMS) ContactMethod.SMS else ContactMethod.EMAIL
        !patient.smsConsent && enabled -> ContactMethod.EMAIL
        patient.smsConsent && !enabled -> ContactMethod.SMS
        else -> ContactMethod.NONE
      }

      val updatedPatients = state.patients.map {
        if (it.id == patientId) {
          it.copy(emailConsent = enabled, preferredContactMethod = nextPreferred)
        } else {
          it
        }
      }
      val prefNote = if (nextPreferred != patient.preferredContactMethod) {
        " (preferred method switched to ${nextPreferred.label})"
      } else {
        ""
      }
      val adminAlerts = if (!enabled) {
        listOf(
          createAdminAlert(
            title = "Consent turned off",
            text = "${patient.name} turned off Email reminder consent.",
            category = NotificationCategory.CONSENT_OFF,
            targetPatientId = patient.id,
          ),
        )
      } else {
        emptyList()
      }
      val logs = appendAuditEntry(
        existingLogs = state.auditLogs,
        who = patient.name,
        role = "Patient",
        action = "Consent changed: Email ${if (enabled) "On" else "Off"}$prefNote",
        outcome = AuditOutcome.ALLOWED,
      )
      state.copy(
        patients = updatedPatients,
        reminderFeedbackByPatientId = state.reminderFeedbackByPatientId - patientId,
        preferencesSavedPatientIds = state.preferencesSavedPatientIds + patientId,
        settingsSaved = true,
        inboxMessages = adminAlerts + state.inboxMessages,
        auditLogs = logs,
        auditIntegrityResult = null,
      )
    }
  }

  fun resetDemoData() {
    val user = _uiState.value.loggedInUser
    if (user?.role != UserRole.ADMIN) {
      updateAndPersist { state ->
        val logs = appendAuditEntry(
          existingLogs = state.auditLogs,
          who = user?.displayName ?: "Unknown",
          role = user?.roleLabel ?: "Unauthenticated",
          action = "Denied attempt to reset demo data",
          outcome = AuditOutcome.DENIED,
        )
        val adminAlert = createAdminAlert(
          title = "Access denied",
          text = "An unauthorized attempt to reset demo data was denied.",
          category = NotificationCategory.ACCESS_DENIED,
        )
        state.copy(
          accessDeniedMessage = "Access denied",
          inboxMessages = listOf(adminAlert) + state.inboxMessages,
          auditLogs = logs,
        )
      }
      return
    }

    storage.clear()
    crypto = CareLoopCrypto()
    CareLoopSoundPlayer.isSoundEnabled = true
    nextAuditId = 1L
    nextMessageId = 1L
    nextRequestId = 1L
    nextBookedId = 1L
    nextFollowUpId = 1L
    nextDoctorFollowUpId = 2L
    nextMissedTaskId = 1L

    val freshPatients = DemoClinicData.createFakePatients(crypto)
    val freshProfiles = DemoClinicData.createInitialProfiles(freshPatients, crypto)
    val freshDocFollowUps = DemoClinicData.createInitialDoctorFollowUps()
    val (freshNotifications, updatedMsgId) = DemoClinicData.createInitialStaffNotifications(
      patients = freshPatients,
      startId = nextMessageId,
    )
    nextMessageId = updatedMsgId
    val resetLogs = appendAuditEntry(
      existingLogs = emptyList(),
      who = user.displayName,
      role = user.roleLabel,
      action = "Demo data reset by Admin",
      outcome = AuditOutcome.ALLOWED,
    )

    val freshState = CareLoopUiState(
      showSplash = false,
      currentScreen = CareLoopScreen.Admin(AdminTab.SETTINGS),
      loggedInUser = user.copy(
        displayName = freshProfiles[DemoClinicData.ACCOUNT_ADMIN]?.name ?: "Clinic Admin",
        email = DemoClinicData.ADMIN_EMAIL,
      ),
      appSettings = AppSettings(),
      patients = freshPatients,
      userProfiles = freshProfiles,
      demoAccounts = DemoClinicData.buildDemoAccounts(freshPatients, freshProfiles),
      inboxMessages = freshNotifications,
      doctorRecommendedFollowUps = freshDocFollowUps,
      auditLogs = resetLogs,
    )
    _uiState.value = freshState
    persistSnapshot(freshState)
  }
}
