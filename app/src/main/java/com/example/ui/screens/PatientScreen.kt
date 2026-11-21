package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppointmentRequest
import com.example.model.AppointmentRequestStatus
import com.example.model.AttendanceOutcome
import com.example.model.BookedAppointment
import com.example.model.CareLoopScreen
import com.example.model.ClinicSchedule
import com.example.model.ClinicSlot
import com.example.model.DoctorId
import com.example.model.DoctorRecommendedFollowUp
import com.example.model.FollowUpAppointment
import com.example.model.InboxMessage
import com.example.model.LoggedInUser
import com.example.model.Patient
import com.example.model.PatientTab
import com.example.model.UserProfile
import com.example.ui.components.BookingWizardSheet
import com.example.ui.components.CareLoopAvatar
import com.example.ui.components.CareLoopDashboardScaffold
import com.example.ui.components.CareLoopOutlinedButton
import com.example.ui.components.CareLoopPrimaryButton
import com.example.ui.components.ClinicDatePickerDialog
import com.example.ui.components.StatusPill
import com.example.ui.theme.BodyInkPrimary
import com.example.ui.theme.BodyInkSecondary
import com.example.ui.theme.CardSurface
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.HeadingDarkGreen
import com.example.ui.theme.NeutralBadgeBg
import com.example.ui.theme.NeutralBadgeText
import com.example.ui.theme.RiskHighBg
import com.example.ui.theme.RiskHighBorder
import com.example.ui.theme.RiskHighText
import com.example.ui.theme.RiskMediumBg
import com.example.ui.theme.RiskMediumBorder
import com.example.ui.theme.RiskMediumText
import com.example.ui.theme.SidebarActiveGreen
import com.example.ui.theme.SidebarDarkGreen
import com.example.ui.theme.SidebarMutedLight
import com.example.ui.theme.SidebarTextLight
import com.example.ui.theme.SoftGreenSurface
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.theme.SuccessGreenBorder
import com.example.ui.theme.SuccessGreenText

@Composable
fun PatientScreen(
  loggedInUser: LoggedInUser,
  userProfile: UserProfile?,
  patient: Patient,
  activeTab: PatientTab,
  followUpAppointments: List<FollowUpAppointment>,
  doctorRecommendedFollowUps: List<DoctorRecommendedFollowUp>,
  bookedAppointments: List<BookedAppointment>,
  appointmentRequests: List<AppointmentRequest>,
  notifications: List<InboxMessage>,
  notificationsOpen: Boolean,
  topToastMessage: String?,
  schedulingError: String?,
  getFreeSlots: (DoctorId, String) -> List<ClinicSlot>,
  onSelectTab: (PatientTab) -> Unit,
  onConfirm: () -> Unit,
  onCancel: () -> Unit,
  onReschedule: (String, Int) -> Unit,
  onSubmitBookingWizard: (
    doctorId: DoctorId,
    dateKey: String,
    slotIndex: Int,
    reason: String,
    privateNote: String,
    fromFollowUpId: Long?,
  ) -> Unit,
  onAcceptOffer: (Long) -> Unit,
  onDeclineOffer: (Long) -> Unit,
  onClearSchedulingError: () -> Unit,
  onOpenNotifications: () -> Unit,
  onCloseNotifications: () -> Unit,
  onAcknowledgeNotification: (Long) -> Unit,
  onMarkAllRead: () -> Unit,
  onNotificationClick: (InboxMessage) -> Unit = {},
  onDismissTopToast: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenProfile: () -> Unit,
  onLogout: () -> Unit,
) {
  var showWizard by remember { mutableStateOf(false) }
  var wizardDoctor by remember { mutableStateOf<DoctorId?>(null) }
  var wizardReason by remember { mutableStateOf("") }
  var wizardFollowUpId by remember { mutableStateOf<Long?>(null) }
  var showRescheduleDialog by remember { mutableStateOf(false) }

  if (showWizard) {
    BookingWizardSheet(
      initialDoctor = wizardDoctor,
      initialReason = wizardReason,
      getFreeSlots = getFreeSlots,
      onOpenSettings = {
        showWizard = false
        onOpenSettings()
      },
      onSubmit = { doctorId, dateKey, slotIndex, reason, privateNote ->
        onSubmitBookingWizard(doctorId, dateKey, slotIndex, reason, privateNote, wizardFollowUpId)
        showWizard = false
        wizardDoctor = null
        wizardReason = ""
        wizardFollowUpId = null
        onSelectTab(PatientTab.APPOINTMENTS)
      },
      onDismiss = {
        showWizard = false
        wizardDoctor = null
        wizardReason = ""
        wizardFollowUpId = null
      },
    )
  }

  if (showRescheduleDialog) {
    RescheduleSlotDialog(
      doctorId = patient.assignedDoctor,
      getFreeSlots = getFreeSlots,
      onConfirmReschedule = { dateKey, slotIndex ->
        onReschedule(dateKey, slotIndex)
        showRescheduleDialog = false
      },
      onDismiss = { showRescheduleDialog = false },
    )
  }

  CareLoopDashboardScaffold(
    loggedInUser = loggedInUser,
    userProfile = userProfile,
    currentScreen = CareLoopScreen.PatientPortal(patient.id, activeTab),
    notifications = notifications,
    notificationsOpen = notificationsOpen,
    topToastMessage = topToastMessage,
    onOpenNotifications = onOpenNotifications,
    onCloseNotifications = onCloseNotifications,
    onAcknowledgeNotification = onAcknowledgeNotification,
    onMarkAllRead = onMarkAllRead,
    onNotificationClick = onNotificationClick,
    onDismissTopToast = onDismissTopToast,
    onOpenSettings = onOpenSettings,
    onOpenProfile = onOpenProfile,
    onSelectPatientTab = onSelectTab,
    onLogout = onLogout,
  ) {
    AnimatedContent(
      targetState = activeTab,
      transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
      label = "patient_tab_transition",
    ) { tab ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Column(
          modifier = Modifier
            .widthIn(max = 680.dp)
            .fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
          if (schedulingError != null) {
            Surface(
              color = RiskHighBg,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, RiskHighBorder),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Outlined.ErrorOutline,
                  contentDescription = null,
                  tint = RiskHighText,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = schedulingError,
                  style = MaterialTheme.typography.bodySmall,
                  color = RiskHighText,
                  modifier = Modifier
                    .weight(1f)
                    .testTag("patient_scheduling_error"),
                )
                TextButton(onClick = onClearSchedulingError) {
                  Text("Dismiss", color = RiskHighText)
                }
              }
            }
          }

          when (tab) {
            PatientTab.HOME -> {
              PatientHomeContent(
                patient = patient,
                doctorRecommendedFollowUps = doctorRecommendedFollowUps.filter { !it.booked },
                followUpAppointments = followUpAppointments,
                onConfirm = onConfirm,
                onOpenReschedule = { showRescheduleDialog = true },
                onBookAppointment = {
                  wizardDoctor = null
                  wizardReason = ""
                  wizardFollowUpId = null
                  showWizard = true
                },
                onViewAllFollowUps = { onSelectTab(PatientTab.FOLLOW_UPS) },
              )
            }

            PatientTab.APPOINTMENTS -> {
              PatientAppointmentsContent(
                patient = patient,
                bookedAppointments = bookedAppointments,
                followUpAppointments = followUpAppointments,
                appointmentRequests = appointmentRequests,
                onConfirm = onConfirm,
                onCancel = onCancel,
                onOpenReschedule = { showRescheduleDialog = true },
                onAcceptOffer = onAcceptOffer,
                onDeclineOffer = onDeclineOffer,
                onBookAppointment = {
                  wizardDoctor = null
                  wizardReason = ""
                  wizardFollowUpId = null
                  showWizard = true
                },
              )
            }

            PatientTab.FOLLOW_UPS -> {
              PatientFollowUpsContent(
                doctorRecommendedFollowUps = doctorRecommendedFollowUps,
                followUpAppointments = followUpAppointments,
                onBookFollowUp = { followUp ->
                  wizardDoctor = followUp.doctorId
                  wizardReason = followUp.reason
                  wizardFollowUpId = followUp.id
                  showWizard = true
                },
              )
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
        }
      }
    }
  }
}

@Composable
private fun PatientHomeContent(
  patient: Patient,
  doctorRecommendedFollowUps: List<DoctorRecommendedFollowUp>,
  followUpAppointments: List<FollowUpAppointment>,
  onConfirm: () -> Unit,
  onOpenReschedule: () -> Unit,
  onBookAppointment: () -> Unit,
  onViewAllFollowUps: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = "Hello, ${patient.firstName}",
        style = MaterialTheme.typography.headlineMedium,
        color = HeadingDarkGreen,
        modifier = Modifier.testTag("patient_greeting"),
      )
      Text(
        text = "Here is your care schedule and upcoming follow-ups.",
        style = MaterialTheme.typography.bodyMedium,
        color = BodyInkSecondary,
      )
    }

    // One prominent dark green card with soft shadow & hover lift
    val cardInteraction = remember { MutableInteractionSource() }
    val isHovered by cardInteraction.collectIsHoveredAsState()
    val elevation by animateDpAsState(
      targetValue = if (isHovered) 10.dp else 5.dp,
      animationSpec = tween(200),
      label = "hero_elevation",
    )
    val scale by animateFloatAsState(
      targetValue = if (isHovered) 1.01f else 1f,
      animationSpec = tween(200),
      label = "hero_scale",
    )

    val needsReschedule = patient.attendanceOutcome == AttendanceOutcome.NO_SHOW ||
      (patient.pastMissedAppointments > 0 && !patient.confirmed && !patient.cancelled && !patient.rescheduled)

    Surface(
      color = SidebarDarkGreen,
      shape = RoundedCornerShape(18.dp),
      shadowElevation = elevation,
      modifier = Modifier
        .fillMaxWidth()
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .clickable(
          interactionSource = cardInteraction,
          indication = null,
          onClick = {},
        )
        .testTag("patient_next_appointment_card"),
    ) {
      Column(
        modifier = Modifier.padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Your next appointment",
            style = MaterialTheme.typography.labelLarge,
            color = SidebarMutedLight,
          )
          when {
            patient.attendanceOutcome == AttendanceOutcome.NO_SHOW -> StatusPill(
              text = "Missed",
              backgroundColor = RiskHighBg,
              textColor = RiskHighText,
              borderColor = RiskHighBorder,
            )
            patient.cancelled -> StatusPill(
              text = "Cancelled",
              backgroundColor = NeutralBadgeBg,
              textColor = NeutralBadgeText,
            )
            patient.confirmed -> StatusPill(
              text = "Confirmed",
              backgroundColor = SuccessGreenBg,
              textColor = SuccessGreenText,
              borderColor = SuccessGreenBorder,
              modifier = Modifier.testTag("patient_confirmed_badge"),
            )
            else -> StatusPill(
              text = "Action needed",
              backgroundColor = RiskMediumBg,
              textColor = RiskMediumText,
              borderColor = RiskMediumBorder,
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          CareLoopAvatar(
            name = patient.assignedDoctor.displayName,
            gender = patient.assignedDoctor.gender,
            size = 48.dp,
          )
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = patient.assignedDoctor.displayName,
              style = MaterialTheme.typography.titleLarge,
              color = Color.White,
            )
            Text(
              text = patient.assignedDoctor.specializationLabel,
              style = MaterialTheme.typography.bodyMedium,
              color = SidebarMutedLight,
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = null,
            tint = SidebarTextLight,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = patient.appointmentTimeLabel,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.testTag("patient_next_appointment_time"),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (!patient.confirmed && !patient.cancelled && patient.attendanceOutcome != AttendanceOutcome.NO_SHOW) {
            Button(
              onClick = onConfirm,
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = HeadingDarkGreen,
              ),
              contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("patient_confirm_button"),
            ) {
              Icon(
                imageVector = Icons.Outlined.CheckCircleOutline,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Confirm",
                style = MaterialTheme.typography.labelLarge,
              )
            }
          }

          if (needsReschedule) {
            OutlinedButton(
              onClick = onOpenReschedule,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("patient_reschedule_button"),
            ) {
              Icon(
                imageVector = Icons.Outlined.EventRepeat,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Reschedule",
                style = MaterialTheme.typography.labelLarge,
              )
            }
          }
        }
      }
    }

    // Follow-ups due list (clean whitespace + dividers, no heavy cards)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Follow-ups due",
          style = MaterialTheme.typography.titleMedium,
          color = HeadingDarkGreen,
        )
        if (doctorRecommendedFollowUps.isNotEmpty() || followUpAppointments.isNotEmpty()) {
          TextButton(onClick = onViewAllFollowUps) {
            Text(
              text = "See all",
              color = HeadingDarkGreen,
              style = MaterialTheme.typography.labelMedium,
            )
          }
        }
      }

      HorizontalDivider(color = HairlineBorder)

      if (doctorRecommendedFollowUps.isEmpty() && followUpAppointments.isEmpty()) {
        Text(
          text = "No follow-ups due right now.",
          style = MaterialTheme.typography.bodyMedium,
          color = BodyInkSecondary,
          modifier = Modifier.padding(vertical = 8.dp),
        )
      } else {
        doctorRecommendedFollowUps.take(3).forEach { item ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp)
              .testTag("home_followup_due_${item.id}"),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Outlined.EventRepeat,
              contentDescription = null,
              tint = HeadingDarkGreen,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "Follow-up due by ${item.doctorId.displayName} (${item.doctorId.specializationLabel}), ${item.reason.lowercase()}, due ${item.dueDateLabel}",
              style = MaterialTheme.typography.bodyMedium,
              color = BodyInkPrimary,
              modifier = Modifier.weight(1f),
            )
          }
          HorizontalDivider(color = HairlineBorder)
        }
        followUpAppointments.take(2).forEach { item ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Outlined.CheckCircleOutline,
              contentDescription = null,
              tint = SuccessGreenText,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "Urgent follow-up scheduled with ${item.doctorId.nameWithSpecialization} · ${item.timeLabel}",
              style = MaterialTheme.typography.bodyMedium,
              color = BodyInkPrimary,
              modifier = Modifier.weight(1f),
            )
          }
          HorizontalDivider(color = HairlineBorder)
        }
      }
    }

    CareLoopPrimaryButton(
      text = "Book an appointment",
      icon = Icons.Outlined.Add,
      onClick = onBookAppointment,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("patient_book_appointment_cta"),
    )
  }
}

@Composable
private fun PatientAppointmentsContent(
  patient: Patient,
  bookedAppointments: List<BookedAppointment>,
  followUpAppointments: List<FollowUpAppointment>,
  appointmentRequests: List<AppointmentRequest>,
  onConfirm: () -> Unit,
  onCancel: () -> Unit,
  onOpenReschedule: () -> Unit,
  onAcceptOffer: (Long) -> Unit,
  onDeclineOffer: (Long) -> Unit,
  onBookAppointment: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Appointments",
        style = MaterialTheme.typography.headlineSmall,
        color = HeadingDarkGreen,
      )
      CareLoopPrimaryButton(
        text = "Book",
        icon = Icons.Outlined.Add,
        onClick = onBookAppointment,
        modifier = Modifier.testTag("appointments_book_button"),
      )
    }

    // Upcoming
    Text(
      text = "Upcoming",
      style = MaterialTheme.typography.titleMedium,
      color = HeadingDarkGreen,
    )
    HorizontalDivider(color = HairlineBorder)

    if (patient.attendanceOutcome == null && !patient.cancelled) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = patient.assignedDoctor.displayName,
              style = MaterialTheme.typography.titleSmall,
              color = HeadingDarkGreen,
            )
            Text(
              text = "${patient.assignedDoctor.specializationLabel} · ${patient.appointmentTimeLabel}",
              style = MaterialTheme.typography.bodyMedium,
              color = BodyInkSecondary,
            )
          }
          StatusPill(
            text = if (patient.confirmed) "Confirmed" else "Unconfirmed",
            backgroundColor = if (patient.confirmed) SuccessGreenBg else RiskMediumBg,
            textColor = if (patient.confirmed) SuccessGreenText else RiskMediumText,
            borderColor = if (patient.confirmed) SuccessGreenBorder else RiskMediumBorder,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          if (!patient.confirmed) {
            CareLoopPrimaryButton(
              text = "Confirm",
              onClick = onConfirm,
              modifier = Modifier.testTag("appointments_confirm_button"),
            )
          }
          CareLoopOutlinedButton(
            text = "Cancel",
            tint = RiskHighText,
            borderColor = RiskHighBorder,
            onClick = onCancel,
            modifier = Modifier.testTag("appointments_cancel_button"),
          )
        }
      }
      HorizontalDivider(color = HairlineBorder)
    }

    bookedAppointments.forEach { appt ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp)
          .testTag("booked_appointment_${appt.id}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = appt.doctorId.nameWithSpecialization,
            style = MaterialTheme.typography.titleSmall,
            color = HeadingDarkGreen,
          )
          Text(
            text = "${appt.timeLabel} · ${appt.reason}",
            style = MaterialTheme.typography.bodySmall,
            color = BodyInkSecondary,
          )
        }
        StatusPill(
          text = "Booked",
          backgroundColor = SuccessGreenBg,
          textColor = SuccessGreenText,
          borderColor = SuccessGreenBorder,
        )
      }
      HorizontalDivider(color = HairlineBorder)
    }

    followUpAppointments.forEach { f ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${f.doctorId.nameWithSpecialization} (Follow-up)",
            style = MaterialTheme.typography.titleSmall,
            color = HeadingDarkGreen,
          )
          Text(
            text = f.timeLabel,
            style = MaterialTheme.typography.bodySmall,
            color = BodyInkSecondary,
          )
        }
        StatusPill(
          text = "Scheduled",
          backgroundColor = SoftGreenSurface,
          textColor = HeadingDarkGreen,
          borderColor = HairlineBorder,
        )
      }
      HorizontalDivider(color = HairlineBorder)
    }

    // Past
    Text(
      text = "Past",
      style = MaterialTheme.typography.titleMedium,
      color = HeadingDarkGreen,
    )
    HorizontalDivider(color = HairlineBorder)

    val historicalMissed = if (patient.attendanceOutcome == AttendanceOutcome.NO_SHOW) {
      val idxToday = patient.missedHistory.indexOfFirst { it.dateKey == patient.appointmentDateKey }
      if (idxToday >= 0) {
        patient.missedHistory.filterIndexed { index, _ -> index != idxToday }
      } else {
        patient.missedHistory
      }
    } else {
      patient.missedHistory
    }

    if (patient.attendanceOutcome == null && !patient.cancelled && historicalMissed.isEmpty()) {
      Text(
        text = "No completed or missed visits yet.",
        style = MaterialTheme.typography.bodySmall,
        color = BodyInkSecondary,
      )
    } else {
      if (patient.attendanceOutcome != null || patient.cancelled) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = patient.assignedDoctor.nameWithSpecialization,
              style = MaterialTheme.typography.titleSmall,
              color = HeadingDarkGreen,
            )
            Text(
              text = patient.appointmentTimeLabel,
              style = MaterialTheme.typography.bodySmall,
              color = BodyInkSecondary,
            )
          }
          if (patient.attendanceOutcome == AttendanceOutcome.NO_SHOW) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              StatusPill(
                text = "Missed",
                backgroundColor = RiskHighBg,
                textColor = RiskHighText,
                borderColor = RiskHighBorder,
              )
              CareLoopOutlinedButton(
                text = "Reschedule",
                onClick = onOpenReschedule,
              )
            }
          } else {
            StatusPill(
              text = if (patient.cancelled) "Cancelled" else "Attended",
              backgroundColor = if (patient.cancelled) NeutralBadgeBg else SuccessGreenBg,
              textColor = if (patient.cancelled) NeutralBadgeText else SuccessGreenText,
            )
          }
        }
        HorizontalDivider(color = HairlineBorder)
      }

      historicalMissed.forEach { record ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("patient_past_missed_row_${record.id}"),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = record.doctorName,
              style = MaterialTheme.typography.titleSmall,
              color = HeadingDarkGreen,
            )
            Text(
              text = record.dateLabel,
              style = MaterialTheme.typography.bodySmall,
              color = BodyInkSecondary,
            )
          }
          StatusPill(
            text = "Missed",
            backgroundColor = RiskHighBg,
            textColor = RiskHighText,
            borderColor = RiskHighBorder,
          )
        }
        HorizontalDivider(color = HairlineBorder)
      }
    }

    // Requests with clear status tracker
    Text(
      text = "Requests",
      style = MaterialTheme.typography.titleMedium,
      color = HeadingDarkGreen,
    )
    HorizontalDivider(color = HairlineBorder)

    if (appointmentRequests.isEmpty()) {
      Text(
        text = "No appointment requests submitted yet.",
        style = MaterialTheme.typography.bodySmall,
        color = BodyInkSecondary,
      )
    } else {
      appointmentRequests.forEach { req ->
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .testTag("patient_request_item_${req.id}"),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = req.doctorId.nameWithSpecialization,
                style = MaterialTheme.typography.titleSmall,
                color = HeadingDarkGreen,
              )
              val timePart = req.preferredSlotIndex?.let { ClinicSchedule.slotLabel(it) }
                ?: req.preferredTimeOfDay.label
              Text(
                text = "${req.preferredDateLabel} · $timePart · ${req.reason}",
                style = MaterialTheme.typography.bodySmall,
                color = BodyInkSecondary,
              )
            }
          }

          RequestStatusTracker(status = req.status)

          when (req.status) {
            AppointmentRequestStatus.BOOKED -> {
              Text(
                text = "Booked for ${req.bookedDateLabel} at ${req.bookedTimeLabel}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = SuccessGreenText,
              )
            }

            AppointmentRequestStatus.NEW_TIME_OFFERED -> {
              Text(
                text = "New time offered: ${req.offeredDateLabel} at ${req.offeredTimeLabel}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = RiskMediumText,
              )
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CareLoopPrimaryButton(
                  text = "Accept",
                  onClick = { onAcceptOffer(req.id) },
                  modifier = Modifier.testTag("accept_offer_button_${req.id}"),
                )
                CareLoopOutlinedButton(
                  text = "Decline",
                  tint = RiskHighText,
                  borderColor = RiskHighBorder,
                  onClick = { onDeclineOffer(req.id) },
                  modifier = Modifier.testTag("decline_offer_button_${req.id}"),
                )
              }
            }

            AppointmentRequestStatus.DECLINED -> {
              Text(
                text = "Declined${req.declineReason?.let { ": $it" } ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = RiskHighText,
              )
            }

            AppointmentRequestStatus.PENDING -> Unit
          }
        }
        HorizontalDivider(color = HairlineBorder)
      }
    }
  }
}

@Composable
private fun RequestStatusTracker(status: AppointmentRequestStatus) {
  val steps = listOf(
    "Request sent",
    "Under review",
    when (status) {
      AppointmentRequestStatus.BOOKED -> "Booked"
      AppointmentRequestStatus.NEW_TIME_OFFERED -> "New time offered"
      AppointmentRequestStatus.DECLINED -> "Declined"
      AppointmentRequestStatus.PENDING -> "Booked"
    },
  )
  val completedUpTo = when (status) {
    AppointmentRequestStatus.PENDING -> 1
    AppointmentRequestStatus.NEW_TIME_OFFERED -> 1
    AppointmentRequestStatus.BOOKED -> 2
    AppointmentRequestStatus.DECLINED -> 2
  }
  val finalIsError = status == AppointmentRequestStatus.DECLINED
  val finalIsAmber = status == AppointmentRequestStatus.NEW_TIME_OFFERED

  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    steps.forEachIndexed { index, label ->
      val active = index <= completedUpTo
      val dotColor = when {
        index == 2 && finalIsError -> RiskHighText
        index == 2 && finalIsAmber -> RiskMediumText
        active -> SidebarDarkGreen
        else -> HairlineBorder
      }
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (index < 2) Modifier.weight(1f) else Modifier,
      ) {
        Box(
          modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(dotColor),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
          ),
          color = if (active) BodyInkPrimary else BodyInkSecondary,
        )
        if (index < 2) {
          HorizontalDivider(
            color = if (index < completedUpTo) SidebarDarkGreen else HairlineBorder,
            thickness = 1.5.dp,
            modifier = Modifier
              .weight(1f)
              .padding(horizontal = 8.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun PatientFollowUpsContent(
  doctorRecommendedFollowUps: List<DoctorRecommendedFollowUp>,
  followUpAppointments: List<FollowUpAppointment>,
  onBookFollowUp: (DoctorRecommendedFollowUp) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = "Follow-ups",
        style = MaterialTheme.typography.headlineSmall,
        color = HeadingDarkGreen,
      )
      Text(
        text = "Follow-up visits recommended by your doctors.",
        style = MaterialTheme.typography.bodyMedium,
        color = BodyInkSecondary,
      )
    }

    HorizontalDivider(color = HairlineBorder)

    if (doctorRecommendedFollowUps.isEmpty() && followUpAppointments.isEmpty()) {
      Text(
        text = "No doctor-recommended follow-ups right now.",
        style = MaterialTheme.typography.bodyMedium,
        color = BodyInkSecondary,
      )
    } else {
      doctorRecommendedFollowUps.forEach { item ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("followup_row_${item.id}"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(
            modifier = Modifier
              .weight(1f)
              .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
          ) {
            Text(
              text = item.doctorId.nameWithSpecialization,
              style = MaterialTheme.typography.titleSmall,
              color = HeadingDarkGreen,
            )
            Text(
              text = "${item.reason} · Due ${item.dueDateLabel}",
              style = MaterialTheme.typography.bodyMedium,
              color = BodyInkPrimary,
            )
          }
          if (item.booked) {
            StatusPill(
              text = "Booked",
              backgroundColor = SuccessGreenBg,
              textColor = SuccessGreenText,
              borderColor = SuccessGreenBorder,
            )
          } else {
            CareLoopPrimaryButton(
              text = "Book",
              onClick = { onBookFollowUp(item) },
              modifier = Modifier.testTag("followup_book_button_${item.id}"),
            )
          }
        }
        HorizontalDivider(color = HairlineBorder)
      }

      followUpAppointments.forEach { scheduled ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = scheduled.doctorId.nameWithSpecialization,
              style = MaterialTheme.typography.titleSmall,
              color = HeadingDarkGreen,
            )
            Text(
              text = "Urgent follow-up · ${scheduled.timeLabel}",
              style = MaterialTheme.typography.bodySmall,
              color = BodyInkSecondary,
            )
          }
          StatusPill(
            text = "Scheduled",
            backgroundColor = SuccessGreenBg,
            textColor = SuccessGreenText,
            borderColor = SuccessGreenBorder,
          )
        }
        HorizontalDivider(color = HairlineBorder)
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RescheduleSlotDialog(
  doctorId: DoctorId,
  getFreeSlots: (DoctorId, String) -> List<ClinicSlot>,
  onConfirmReschedule: (String, Int) -> Unit,
  onDismiss: () -> Unit,
) {
  var selectedDateKey by remember {
    mutableStateOf(
      ClinicSchedule.openDateKeysWithin(startOffsetDays = 1, daysWindow = 10).firstOrNull()
        ?: ClinicSchedule.todayLocalDateKey()
    )
  }
  var selectedSlotIndex by remember { mutableStateOf<Int?>(null) }
  var showDatePicker by remember { mutableStateOf(false) }
  val freeSlots = remember(doctorId, selectedDateKey) { getFreeSlots(doctorId, selectedDateKey) }

  if (showDatePicker) {
    ClinicDatePickerDialog(
      initialDateKey = selectedDateKey,
      onDateSelected = {
        selectedDateKey = it
        selectedSlotIndex = null
        showDatePicker = false
      },
      onDismiss = { showDatePicker = false },
    )
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = CardSurface,
    title = {
      Text(
        text = "Reschedule with ${doctorId.displayName}",
        style = MaterialTheme.typography.titleLarge,
        color = HeadingDarkGreen,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = doctorId.specializationLabel,
          style = MaterialTheme.typography.bodySmall,
          color = BodyInkSecondary,
        )
        OutlinedButton(
          onClick = { showDatePicker = true },
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, HairlineBorder),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(
            imageVector = Icons.Outlined.CalendarToday,
            contentDescription = null,
            tint = HeadingDarkGreen,
            modifier = Modifier.size(17.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = ClinicSchedule.formatDateLabel(selectedDateKey),
            color = HeadingDarkGreen,
          )
        }
        if (freeSlots.isEmpty()) {
          Text(
            text = "No free slots on this date.",
            style = MaterialTheme.typography.bodySmall,
            color = RiskHighText,
          )
        } else {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            freeSlots.forEach { slot ->
              val selected = selectedSlotIndex == slot.index
              Surface(
                color = if (selected) SidebarDarkGreen else SoftGreenSurface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (selected) SidebarDarkGreen else HairlineBorder),
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { selectedSlotIndex = slot.index }
                  .testTag("reschedule_slot_${slot.index}"),
              ) {
                Text(
                  text = slot.label,
                  style = MaterialTheme.typography.labelMedium,
                  color = if (selected) Color.White else HeadingDarkGreen,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      CareLoopPrimaryButton(
        text = "Reschedule",
        enabled = selectedSlotIndex != null,
        onClick = {
          val slot = selectedSlotIndex ?: return@CareLoopPrimaryButton
          onConfirmReschedule(selectedDateKey, slot)
        },
        modifier = Modifier.testTag("reschedule_confirm_button"),
      )
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = BodyInkSecondary)
      }
    },
  )
}
