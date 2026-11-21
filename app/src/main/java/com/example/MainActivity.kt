package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.FirebaseCareLoopAiGateway
import com.example.audio.CareLoopSoundPlayer
import com.example.data.SharedPrefsCareLoopStorage
import com.example.model.CareLoopScreen
import com.example.security.configureAppCheck
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.DoctorScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PatientScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReceptionistScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.CareLoopTheme
import com.example.viewmodel.CareLoopViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureAppCheck(this, intent)
    title = getString(R.string.window_title)
    enableEdgeToEdge()
    setContent {
      CareLoopTheme {
        CareLoopApp()
      }
    }
  }
}

@Composable
fun CareLoopApp() {
  val appContext = LocalContext.current.applicationContext
  val factory = remember(appContext) {
    object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CareLoopViewModel(
          aiGateway = FirebaseCareLoopAiGateway(),
          storage = SharedPrefsCareLoopStorage(appContext),
        ) as T
      }
    }
  }
  val viewModel: CareLoopViewModel = viewModel(factory = factory)
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val loggedInUser = uiState.loggedInUser
  val userProfile = loggedInUser?.let { uiState.userProfiles[it.accountKey] }
  val notifications = viewModel.notificationsForCurrentUser(uiState)

  Box(
    modifier = Modifier
      .fillMaxSize()
      .pointerInput(Unit) {
        awaitPointerEventScope {
          while (true) {
            awaitPointerEvent(PointerEventPass.Initial)
            CareLoopSoundPlayer.markUserInteracted()
          }
        }
      },
  ) {
    Crossfade(
      targetState = uiState.showSplash,
      animationSpec = tween(durationMillis = 280),
      label = "splash_to_app",
    ) { showingSplash ->
      if (showingSplash) {
        SplashScreen(onFinished = viewModel::finishSplash)
      } else {
        AnimatedContent(
          targetState = uiState.currentScreen,
          transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(180)) },
          contentKey = { screen ->
            when (screen) {
              is CareLoopScreen.Login -> "login"
              is CareLoopScreen.Receptionist -> "receptionist"
              is CareLoopScreen.Doctor -> "doctor_${screen.doctorId.name}"
              is CareLoopScreen.PatientPortal -> "patient_${screen.patientId}"
              is CareLoopScreen.Admin -> "admin"
              is CareLoopScreen.Profile -> "profile"
              is CareLoopScreen.Settings -> "settings"
            }
          },
          label = "screen_transition",
        ) { screen ->
          when (screen) {
            is CareLoopScreen.Login -> {
              LoginScreen(
                patients = uiState.patients,
                userProfiles = uiState.userProfiles,
                loginError = uiState.loginError,
                onEmailOrPasswordEdited = viewModel::clearLoginError,
                onSignIn = viewModel::signIn,
              )
            }

            is CareLoopScreen.Receptionist -> {
              if (loggedInUser != null) {
                ReceptionistScreen(
                  loggedInUser = loggedInUser,
                  userProfile = userProfile,
                  activeTab = screen.tab,
                  patients = uiState.patients,
                  decryptedContactsByPatientId = uiState.decryptedContactsByPatientId,
                  appointmentRequests = uiState.appointmentRequests,
                  missedFollowUpTasks = uiState.missedFollowUpTasks,
                  extraAppointmentsCount = uiState.bookedAppointments.size + uiState.followUpAppointments.size,
                  feedbackByPatientId = uiState.reminderFeedbackByPatientId,
                  reminderStatusByPatientId = uiState.reminderStatusByPatientId,
                  aiDraftsByPatientId = uiState.aiReminderDraftsByPatientId,
                  aiDraftLoadingPatientIds = uiState.aiDraftLoadingPatientIds,
                  queueSort = uiState.appSettings.receptionistQueueSort,
                  highRiskFirst = uiState.appSettings.receptionistHighRiskFirst,
                  notifications = notifications,
                  notificationsOpen = uiState.notificationsOpen,
                  topToastMessage = uiState.topToastMessage,
                  accessDeniedMessage = uiState.accessDeniedMessage,
                  schedulingError = uiState.schedulingError,
                  getFreeSlots = { doctorId, dateKey, timeOfDay ->
                    viewModel.freeSlotsFor(doctorId, dateKey, timeOfDay, uiState)
                  },
                  onSelectTab = viewModel::selectReceptionistTab,
                  onSendReminder = viewModel::sendReminder,
                  onDraftReminderWithAi = viewModel::draftReminderWithAi,
                  onStartEditAiDraft = viewModel::startEditingAiDraft,
                  onUpdateAiDraftText = viewModel::updateAiDraftText,
                  onFinishEditAiDraft = viewModel::finishEditingAiDraft,
                  onApproveAndSendAiDraft = viewModel::approveAndSendAiDraft,
                  onDiscardAiDraft = viewModel::discardAiDraft,
                  onSendMissedFollowUp = viewModel::sendMissedFollowUpMessage,
                  onBookSlot = viewModel::bookAppointmentRequestSlot,
                  onOfferAnotherTime = viewModel::offerAnotherTime,
                  onDeclineRequest = viewModel::declineAppointmentRequest,
                  onClearSchedulingError = viewModel::clearSchedulingError,
                  onOpenNotifications = viewModel::openNotificationsPanel,
                  onCloseNotifications = viewModel::closeNotificationsPanel,
                  onAcknowledgeNotification = viewModel::acknowledgeNotification,
                  onMarkAllRead = viewModel::markAllNotificationsRead,
                  onNotificationClick = { msg -> viewModel.openNotificationTarget(msg.id) },
                  onDismissTopToast = viewModel::dismissTopToast,
                  onOpenSettings = viewModel::openSettings,
                  onOpenProfile = viewModel::openProfile,
                  onLogout = viewModel::logout,
                )
              }
            }

            is CareLoopScreen.Doctor -> {
              if (loggedInUser != null) {
                DoctorScreen(
                  loggedInUser = loggedInUser,
                  userProfile = userProfile,
                  doctorId = screen.doctorId,
                  activeTab = screen.tab,
                  patients = uiState.patients,
                  decryptedMedicalNotesByPatientId = uiState.decryptedMedicalNotesByPatientId,
                  chatMessages = uiState.doctorChatMessagesByDoctorId[screen.doctorId].orEmpty(),
                  isChatLoading = screen.doctorId in uiState.doctorChatLoadingDoctorIds,
                  defaultFollowUpDueDays = uiState.appSettings.doctorDefaultFollowUpDays,
                  aiAssistantVisible = uiState.appSettings.doctorAiAssistantVisible,
                  focusedPatientId = uiState.focusedDoctorPatientId,
                  notifications = notifications,
                  notificationsOpen = uiState.notificationsOpen,
                  topToastMessage = uiState.topToastMessage,
                  accessDeniedMessage = uiState.accessDeniedMessage,
                  onSelectTab = viewModel::selectDoctorTab,
                  onDecision = { patientId, decision ->
                    viewModel.setDoctorDecision(screen.doctorId, patientId, decision)
                  },
                  onRecordAttendance = { patientId, outcome ->
                    viewModel.recordAttendance(screen.doctorId, patientId, outcome)
                  },
                  onCreateFollowUp = { patientId, reason, dueInDays ->
                    viewModel.createDoctorFollowUp(screen.doctorId, patientId, reason, dueInDays)
                  },
                  onAskAiAssistant = { question ->
                    viewModel.askDoctorAiAssistant(screen.doctorId, question)
                  },
                  onClearFocusedPatient = viewModel::clearFocusedDoctorPatient,
                  onOpenNotifications = viewModel::openNotificationsPanel,
                  onCloseNotifications = viewModel::closeNotificationsPanel,
                  onAcknowledgeNotification = viewModel::acknowledgeNotification,
                  onMarkAllRead = viewModel::markAllNotificationsRead,
                  onNotificationClick = { msg -> viewModel.openNotificationTarget(msg.id) },
                  onDismissTopToast = viewModel::dismissTopToast,
                  onOpenSettings = viewModel::openSettings,
                  onOpenProfile = viewModel::openProfile,
                  onLogout = viewModel::logout,
                )
              }
            }

            is CareLoopScreen.PatientPortal -> {
              if (loggedInUser != null) {
                val patient = uiState.patients.find { it.id == screen.patientId }
                  ?: uiState.patients.first()
                PatientScreen(
                  loggedInUser = loggedInUser,
                  userProfile = userProfile,
                  patient = patient,
                  activeTab = screen.tab,
                  followUpAppointments = uiState.followUpAppointments.filter { it.patientId == patient.id },
                  doctorRecommendedFollowUps = uiState.doctorRecommendedFollowUps.filter { it.patientId == patient.id },
                  bookedAppointments = uiState.bookedAppointments.filter { it.patientId == patient.id },
                  appointmentRequests = uiState.appointmentRequests.filter { it.patientId == patient.id },
                  notifications = notifications,
                  notificationsOpen = uiState.notificationsOpen,
                  topToastMessage = uiState.topToastMessage,
                  schedulingError = uiState.schedulingError,
                  getFreeSlots = { doctorId, dateKey ->
                    viewModel.freeSlotsFor(doctorId, dateKey, null, uiState)
                  },
                  onSelectTab = viewModel::selectPatientTab,
                  onConfirm = { viewModel.confirmAppointment(patient.id) },
                  onCancel = { viewModel.cancelAppointment(patient.id) },
                  onReschedule = { dateKey, slotIndex ->
                    viewModel.rescheduleAppointment(patient.id, dateKey, slotIndex)
                  },
                  onSubmitBookingWizard = { doctorId, dateKey, slotIndex, reason, privateNote, fromFollowUpId ->
                    val timeOfDay = com.example.model.ClinicSchedule.ALL_SLOTS
                      .getOrNull(slotIndex)?.timeOfDay ?: com.example.model.TimeOfDay.MORNING
                    viewModel.submitAppointmentRequest(
                      patientId = patient.id,
                      preferredDateKey = dateKey,
                      preferredTimeOfDay = timeOfDay,
                      reason = reason,
                      doctorId = doctorId,
                      preferredSlotIndex = slotIndex,
                      privateNote = privateNote,
                      fromFollowUpId = fromFollowUpId,
                    )
                  },
                  onAcceptOffer = viewModel::acceptOfferedTime,
                  onDeclineOffer = viewModel::declineOfferedTime,
                  onClearSchedulingError = viewModel::clearSchedulingError,
                  onOpenNotifications = viewModel::openNotificationsPanel,
                  onCloseNotifications = viewModel::closeNotificationsPanel,
                  onAcknowledgeNotification = viewModel::acknowledgeNotification,
                  onMarkAllRead = viewModel::markAllNotificationsRead,
                  onNotificationClick = { msg -> viewModel.openNotificationTarget(msg.id) },
                  onDismissTopToast = viewModel::dismissTopToast,
                  onOpenSettings = viewModel::openSettings,
                  onOpenProfile = viewModel::openProfile,
                  onLogout = viewModel::logout,
                )
              }
            }

            is CareLoopScreen.Admin -> {
              if (loggedInUser != null) {
                AdminScreen(
                  loggedInUser = loggedInUser,
                  userProfile = userProfile,
                  activeTab = screen.tab,
                  patients = uiState.patients,
                  appointmentRequests = uiState.appointmentRequests,
                  bookedCount = uiState.bookedAppointments.size,
                  followUpCount = uiState.followUpAppointments.size,
                  auditLogs = uiState.auditLogs,
                  auditIntegrityResult = uiState.auditIntegrityResult,
                  appSettings = uiState.appSettings,
                  settingsSaved = uiState.settingsSaved,
                  notifications = notifications,
                  notificationsOpen = uiState.notificationsOpen,
                  topToastMessage = uiState.topToastMessage,
                  accessDeniedMessage = uiState.accessDeniedMessage,
                  onSelectTab = viewModel::selectAdminTab,
                  onVerifyIntegrity = viewModel::verifyAuditLogIntegrity,
                  onResetDemoData = viewModel::resetDemoData,
                  onNotificationSoundChange = viewModel::updateNotificationSound,
                  onPopupAlertsChange = viewModel::updatePopupAlerts,
                  onOpenNotifications = viewModel::openNotificationsPanel,
                  onCloseNotifications = viewModel::closeNotificationsPanel,
                  onAcknowledgeNotification = viewModel::acknowledgeNotification,
                  onMarkAllRead = viewModel::markAllNotificationsRead,
                  onNotificationClick = { msg -> viewModel.openNotificationTarget(msg.id) },
                  onDismissTopToast = viewModel::dismissTopToast,
                  onOpenSettings = viewModel::openSettings,
                  onOpenProfile = viewModel::openProfile,
                  onLogout = viewModel::logout,
                )
              }
            }

            is CareLoopScreen.Profile -> {
              if (loggedInUser != null) {
                ProfileScreen(
                  loggedInUser = loggedInUser,
                  userProfile = userProfile,
                  currentScreen = screen,
                  notifications = notifications,
                  notificationsOpen = uiState.notificationsOpen,
                  topToastMessage = uiState.topToastMessage,
                  profileSaved = uiState.profileSaved,
                  onBack = viewModel::navigateBackFromSubScreen,
                  onSaveProfile = viewModel::updateUserProfile,
                  onProfileEdited = viewModel::clearProfileSaved,
                  onOpenNotifications = viewModel::openNotificationsPanel,
                  onCloseNotifications = viewModel::closeNotificationsPanel,
                  onAcknowledgeNotification = viewModel::acknowledgeNotification,
                  onMarkAllRead = viewModel::markAllNotificationsRead,
                  onNotificationClick = { msg -> viewModel.openNotificationTarget(msg.id) },
                  onDismissTopToast = viewModel::dismissTopToast,
                  onOpenSettings = viewModel::openSettings,
                  onLogout = viewModel::logout,
                )
              }
            }

            is CareLoopScreen.Settings -> {
              if (loggedInUser != null) {
                val patient = loggedInUser.patientId?.let { pid ->
                  uiState.patients.find { it.id == pid }
                }
                SettingsScreen(
                  loggedInUser = loggedInUser,
                  userProfile = userProfile,
                  patient = patient,
                  appSettings = uiState.appSettings,
                  settingsSaved = uiState.settingsSaved,
                  preferencesSaved = patient?.id?.let { it in uiState.preferencesSavedPatientIds } == true,
                  currentScreen = screen,
                  notifications = notifications,
                  notificationsOpen = uiState.notificationsOpen,
                  topToastMessage = topToastMessageOrNull(uiState.topToastMessage),
                  onBack = viewModel::navigateBackFromSubScreen,
                  onNotificationSoundChange = viewModel::updateNotificationSound,
                  onPopupAlertsChange = viewModel::updatePopupAlerts,
                  onReceptionistSortChange = viewModel::updateReceptionistQueueSort,
                  onReceptionistHighRiskFirstChange = viewModel::updateReceptionistHighRiskFirst,
                  onDoctorDefaultFollowUpDaysChange = viewModel::updateDoctorDefaultFollowUpDays,
                  onDoctorAiAssistantVisibleChange = viewModel::updateDoctorAiAssistantVisible,
                  onPreferredMethodChange = { method ->
                    patient?.id?.let { viewModel.updatePreferredContactMethod(it, method) }
                  },
                  onSmsConsentChange = { enabled ->
                    patient?.id?.let { viewModel.updateSmsConsent(it, enabled) }
                  },
                  onEmailConsentChange = { enabled ->
                    patient?.id?.let { viewModel.updateEmailConsent(it, enabled) }
                  },
                  onOpenNotifications = viewModel::openNotificationsPanel,
                  onCloseNotifications = viewModel::closeNotificationsPanel,
                  onAcknowledgeNotification = viewModel::acknowledgeNotification,
                  onMarkAllRead = viewModel::markAllNotificationsRead,
                  onNotificationClick = { msg -> viewModel.openNotificationTarget(msg.id) },
                  onDismissTopToast = viewModel::dismissTopToast,
                  onOpenProfile = viewModel::openProfile,
                  onLogout = viewModel::logout,
                )
              }
            }
          }
        }
      }
    }
  }
}

private fun topToastMessageOrNull(message: String?): String? = message
