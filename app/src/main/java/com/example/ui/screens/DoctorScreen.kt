package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AttendanceOutcome
import com.example.model.CareLoopScreen
import com.example.model.DoctorChatMessage
import com.example.model.DoctorDecision
import com.example.model.DoctorId
import com.example.model.DoctorTab
import com.example.model.InboxMessage
import com.example.model.LoggedInUser
import com.example.model.Patient
import com.example.model.RiskTier
import com.example.model.UserProfile
import com.example.ui.components.CareLoopAvatar
import com.example.ui.components.CareLoopDashboardScaffold
import com.example.ui.components.CareLoopOutlinedButton
import com.example.ui.components.CareLoopPrimaryButton
import com.example.ui.components.StatusPill
import com.example.ui.theme.BodyInkPrimary
import com.example.ui.theme.BodyInkSecondary
import com.example.ui.theme.CardSurface
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.HeadingDarkGreen
import com.example.ui.theme.RiskHighBg
import com.example.ui.theme.RiskHighBorder
import com.example.ui.theme.RiskHighText
import com.example.ui.theme.RiskMediumBg
import com.example.ui.theme.RiskMediumBorder
import com.example.ui.theme.RiskMediumText
import com.example.ui.theme.SidebarDarkGreen
import com.example.ui.theme.SoftGreenSurface
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.theme.SuccessGreenBorder
import com.example.ui.theme.SuccessGreenText
import com.example.ui.theme.TableHeaderSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorScreen(
  loggedInUser: LoggedInUser,
  userProfile: UserProfile?,
  doctorId: DoctorId,
  activeTab: DoctorTab,
  patients: List<Patient>,
  decryptedMedicalNotesByPatientId: Map<Int, String>,
  chatMessages: List<DoctorChatMessage>,
  isChatLoading: Boolean,
  defaultFollowUpDueDays: Int = 7,
  aiAssistantVisible: Boolean = true,
  focusedPatientId: Int? = null,
  notifications: List<InboxMessage>,
  notificationsOpen: Boolean,
  topToastMessage: String?,
  accessDeniedMessage: String?,
  onSelectTab: (DoctorTab) -> Unit,
  onDecision: (Int, DoctorDecision) -> Unit,
  onRecordAttendance: (Int, AttendanceOutcome) -> Unit,
  onCreateFollowUp: (Int, String, Int) -> Unit,
  onAskAiAssistant: (String) -> Unit,
  onClearFocusedPatient: () -> Unit = {},
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
  var followUpTarget by remember { mutableStateOf<Patient?>(null) }
  var aiAssistantOpen by remember { mutableStateOf(false) }

  followUpTarget?.let { target ->
    CreateFollowUpDialog(
      patient = target,
      initialDueDays = defaultFollowUpDueDays,
      onConfirm = { reason, dueInDays ->
        onCreateFollowUp(target.id, reason, dueInDays)
        followUpTarget = null
      },
      onDismiss = { followUpTarget = null },
    )
  }

  if (aiAssistantOpen) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = { aiAssistantOpen = false },
      sheetState = sheetState,
      containerColor = CardSurface,
    ) {
      DoctorAiAssistantSheetContent(
        doctorId = doctorId,
        messages = chatMessages,
        isLoading = isChatLoading,
        onSend = onAskAiAssistant,
        onClose = { aiAssistantOpen = false },
      )
    }
  }

  val doctorPatients = remember(patients, doctorId) {
    patients
      .filter { it.assignedDoctor == doctorId }
      .sortedWith(
        compareBy<Patient> { it.appointmentDateKey }
          .thenBy { it.appointmentSlotIndex }
      )
  }

  val needsDecisionPatients = remember(doctorPatients) {
    doctorPatients.filter { it.isUrgent && it.doctorDecision == null }
  }

  val todayPatients = remember(doctorPatients) {
    doctorPatients.filter { !it.isUrgent || it.doctorDecision != null }
  }

  CareLoopDashboardScaffold(
    loggedInUser = loggedInUser,
    userProfile = userProfile,
    currentScreen = CareLoopScreen.Doctor(doctorId, activeTab),
    notifications = notifications,
    notificationsOpen = notificationsOpen,
    topToastMessage = topToastMessage,
    accessDeniedMessage = accessDeniedMessage,
    onOpenNotifications = onOpenNotifications,
    onCloseNotifications = onCloseNotifications,
    onAcknowledgeNotification = onAcknowledgeNotification,
    onMarkAllRead = onMarkAllRead,
    onNotificationClick = onNotificationClick,
    onDismissTopToast = onDismissTopToast,
    onOpenSettings = onOpenSettings,
    onOpenProfile = onOpenProfile,
    onSelectDoctorTab = onSelectTab,
    onLogout = onLogout,
    floatingActionButton = {
      if (aiAssistantVisible) {
        DoctorAiPulseButton(
          onClick = { aiAssistantOpen = true },
          modifier = Modifier.testTag("doctor_ai_fab"),
        )
      }
    },
  ) {
    AnimatedContent(
      targetState = activeTab,
      transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
      label = "doctor_tab_transition",
    ) { tab ->
      when (tab) {
        DoctorTab.TODAY -> {
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .testTag("doctor_today_list"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            item {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = "Today's Appointments",
                  style = MaterialTheme.typography.headlineSmall,
                  color = HeadingDarkGreen,
                )
                Text(
                  text = "${doctorId.displayName} · ${doctorId.specializationLabel}",
                  style = MaterialTheme.typography.bodySmall,
                  color = BodyInkSecondary,
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              HorizontalDivider(color = HairlineBorder)
            }

            if (todayPatients.isEmpty()) {
              item {
                Text(
                  text = "No appointments assigned today.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = BodyInkSecondary,
                  modifier = Modifier.padding(vertical = 12.dp),
                )
              }
            } else {
              items(todayPatients, key = { it.id }) { patient ->
                DoctorTodayRow(
                  patient = patient,
                  decryptedNote = decryptedMedicalNotesByPatientId[patient.id],
                  onRecordAttendance = { outcome -> onRecordAttendance(patient.id, outcome) },
                  onCreateFollowUp = { followUpTarget = patient },
                )
                HorizontalDivider(color = HairlineBorder)
              }
            }
          }
        }

        DoctorTab.NEEDS_DECISION -> {
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .testTag("doctor_needs_decision_list"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            item {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = "Needs Decision",
                  style = MaterialTheme.typography.headlineSmall,
                  color = HeadingDarkGreen,
                )
                Text(
                  text = "Urgent follow-ups awaiting your approval or override.",
                  style = MaterialTheme.typography.bodySmall,
                  color = BodyInkSecondary,
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              HorizontalDivider(color = HairlineBorder)
            }

            if (needsDecisionPatients.isEmpty()) {
              item {
                Text(
                  text = "All urgent patients have been reviewed.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = BodyInkSecondary,
                  modifier = Modifier.padding(vertical = 12.dp),
                )
              }
            } else {
              items(needsDecisionPatients, key = { it.id }) { patient ->
                DoctorDecisionRow(
                  patient = patient,
                  decryptedNote = decryptedMedicalNotesByPatientId[patient.id],
                  onDecision = { decision -> onDecision(patient.id, decision) },
                  onCreateFollowUp = { followUpTarget = patient },
                )
                HorizontalDivider(color = HairlineBorder)
              }
            }
          }
        }

        DoctorTab.PATIENTS -> {
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .testTag("doctor_patients_list"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            item {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = "Your Patients",
                  style = MaterialTheme.typography.headlineSmall,
                  color = HeadingDarkGreen,
                )
                Text(
                  text = "Tap a patient to view missed-appointment history and encrypted notes.",
                  style = MaterialTheme.typography.bodySmall,
                  color = BodyInkSecondary,
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              HorizontalDivider(color = HairlineBorder)
            }

            items(doctorPatients, key = { it.id }) { patient ->
              DoctorPatientDirectoryRow(
                patient = patient,
                isFocused = focusedPatientId == patient.id,
                onExpandToggle = {
                  if (focusedPatientId == patient.id) onClearFocusedPatient()
                },
                decryptedNote = decryptedMedicalNotesByPatientId[patient.id],
                onCreateFollowUp = { followUpTarget = patient },
              )
              HorizontalDivider(color = HairlineBorder)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DoctorTodayRow(
  patient: Patient,
  decryptedNote: String?,
  onRecordAttendance: (AttendanceOutcome) -> Unit,
  onCreateFollowUp: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 10.dp)
      .testTag("doctor_today_row_${patient.id}"),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      CareLoopAvatar(name = patient.name, gender = patient.gender, size = 40.dp)
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "${patient.name} · Age ${patient.age}",
          style = MaterialTheme.typography.titleSmall,
          color = HeadingDarkGreen,
        )
        Text(
          text = patient.appointmentTimeLabel,
          style = MaterialTheme.typography.bodySmall,
          color = BodyInkSecondary,
        )
      }
      RiskBadgeSmall(patient = patient)
    }

    if (!decryptedNote.isNullOrBlank()) {
      ViewMedicalNoteLink(
        patientId = patient.id,
        note = decryptedNote,
      )
    }

    if (patient.attendanceOutcome != null) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val attended = patient.attendanceOutcome == AttendanceOutcome.ATTENDED
        StatusPill(
          text = "${patient.attendanceOutcome.label} · ${patient.attendanceRecordedAt.orEmpty()}",
          backgroundColor = if (attended) SuccessGreenBg else RiskHighBg,
          textColor = if (attended) SuccessGreenText else RiskHighText,
          borderColor = if (attended) SuccessGreenBorder else RiskHighBorder,
          modifier = Modifier.testTag("attendance_badge_${patient.id}"),
        )
        CareLoopOutlinedButton(
          text = "Create follow-up",
          icon = Icons.Outlined.EventRepeat,
          onClick = onCreateFollowUp,
          modifier = Modifier.testTag("doctor_followup_button_${patient.id}"),
        )
      }
    } else {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CareLoopPrimaryButton(
          text = "Attended",
          onClick = { onRecordAttendance(AttendanceOutcome.ATTENDED) },
          modifier = Modifier.testTag("attended_button_${patient.id}"),
        )
        CareLoopOutlinedButton(
          text = "No-show",
          tint = RiskHighText,
          borderColor = RiskHighBorder,
          onClick = { onRecordAttendance(AttendanceOutcome.NO_SHOW) },
          modifier = Modifier.testTag("no_show_button_${patient.id}"),
        )
        CareLoopOutlinedButton(
          text = "Create follow-up",
          onClick = onCreateFollowUp,
          modifier = Modifier.testTag("doctor_followup_button_${patient.id}"),
        )
      }
    }
  }
}

@Composable
private fun DoctorDecisionRow(
  patient: Patient,
  decryptedNote: String?,
  onDecision: (DoctorDecision) -> Unit,
  onCreateFollowUp: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 10.dp)
      .testTag("doctor_decision_row_${patient.id}"),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      CareLoopAvatar(name = patient.name, gender = patient.gender, size = 40.dp)
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "${patient.name} · Age ${patient.age}",
          style = MaterialTheme.typography.titleSmall,
          color = HeadingDarkGreen,
        )
        Text(
          text = "${patient.appointmentTimeLabel} · Urgent follow-up",
          style = MaterialTheme.typography.bodySmall,
          color = RiskHighText,
        )
      }
      RiskBadgeSmall(patient = patient)
    }

    if (!decryptedNote.isNullOrBlank()) {
      ViewMedicalNoteLink(
        patientId = patient.id,
        note = decryptedNote,
      )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      CareLoopPrimaryButton(
        text = stringResource(R.string.approve),
        onClick = { onDecision(DoctorDecision.APPROVED) },
        modifier = Modifier.testTag("doctor_approve_button_${patient.id}"),
      )
      CareLoopOutlinedButton(
        text = stringResource(R.string.override_action),
        onClick = { onDecision(DoctorDecision.OVERRIDDEN) },
        modifier = Modifier.testTag("doctor_override_button_${patient.id}"),
      )
      CareLoopOutlinedButton(
        text = "Create follow-up",
        onClick = onCreateFollowUp,
        modifier = Modifier.testTag("doctor_followup_button_${patient.id}"),
      )
    }
  }
}

@Composable
private fun DoctorPatientDirectoryRow(
  patient: Patient,
  isFocused: Boolean,
  onExpandToggle: () -> Unit,
  decryptedNote: String?,
  onCreateFollowUp: () -> Unit,
) {
  var expanded by remember(patient.id) { mutableStateOf(isFocused) }
  LaunchedEffect(isFocused) {
    if (isFocused) expanded = true
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable {
        expanded = !expanded
        onExpandToggle()
      }
      .padding(vertical = 10.dp)
      .testTag("doctor_patient_directory_row_${patient.id}"),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      CareLoopAvatar(name = patient.name, gender = patient.gender, size = 38.dp)
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = patient.name,
          style = MaterialTheme.typography.titleSmall,
          color = HeadingDarkGreen,
        )
        Text(
          text = "Age ${patient.age} · ${patient.appointmentTimeLabel}",
          style = MaterialTheme.typography.bodySmall,
          color = BodyInkSecondary,
        )
      }
      RiskBadgeSmall(patient = patient)
    }

    AnimatedVisibility(
      visible = expanded,
      enter = fadeIn(tween(180)) + expandVertically(tween(200)),
      exit = fadeOut(tween(150)) + shrinkVertically(tween(180)),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = patient.riskReasonSummary(),
          style = MaterialTheme.typography.bodySmall,
          color = BodyInkSecondary,
        )
        Text(
          text = "Missed appointment history",
          style = MaterialTheme.typography.labelLarge,
          color = HeadingDarkGreen,
          modifier = Modifier.testTag("doctor_missed_history_header_${patient.id}"),
        )
        if (patient.missedHistory.isEmpty()) {
          Text(
            text = "No missed appointments on record.",
            style = MaterialTheme.typography.bodySmall,
            color = BodyInkSecondary,
          )
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            patient.missedHistory.forEach { record ->
              Surface(
                color = TableHeaderSurface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("doctor_missed_history_row_${patient.id}_${record.id}"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = record.dateLabel,
                      style = MaterialTheme.typography.labelLarge,
                      color = BodyInkPrimary,
                    )
                    Text(
                      text = record.doctorName,
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
              }
            }
          }
        }
        if (!decryptedNote.isNullOrBlank()) {
          ViewMedicalNoteLink(
            patientId = patient.id,
            note = decryptedNote,
          )
        }
        CareLoopOutlinedButton(
          text = "Create follow-up",
          icon = Icons.Outlined.EventRepeat,
          onClick = onCreateFollowUp,
          modifier = Modifier.testTag("doctor_followup_button_${patient.id}"),
        )
      }
    }
  }
}

@Composable
private fun ViewMedicalNoteLink(
  patientId: Int,
  note: String,
) {
  var revealed by remember { mutableStateOf(false) }
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(
      modifier = Modifier
        .clip(RoundedCornerShape(6.dp))
        .clickable { revealed = !revealed }
        .padding(vertical = 4.dp)
        .testTag("view_note_link_$patientId"),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Outlined.Lock,
        contentDescription = null,
        tint = HeadingDarkGreen,
        modifier = Modifier.size(14.dp),
      )
      Spacer(modifier = Modifier.width(5.dp))
      Text(
        text = if (revealed) "Hide note" else "View note",
        style = MaterialTheme.typography.labelMedium.copy(
          textDecoration = TextDecoration.Underline,
        ),
        color = HeadingDarkGreen,
      )
    }

    AnimatedVisibility(visible = revealed) {
      Surface(
        color = TableHeaderSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = note,
          style = MaterialTheme.typography.bodySmall,
          color = BodyInkPrimary,
          modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("medical_note_text_$patientId"),
        )
      }
    }
  }
}

@Composable
private fun RiskBadgeSmall(patient: Patient) {
  val tier = patient.riskTier()
  val (bg, text, border) = when (tier) {
    RiskTier.HIGH -> Triple(RiskHighBg, RiskHighText, RiskHighBorder)
    RiskTier.MEDIUM -> Triple(RiskMediumBg, RiskMediumText, RiskMediumBorder)
    RiskTier.LOW -> Triple(SuccessGreenBg, SuccessGreenText, SuccessGreenBorder)
  }
  StatusPill(
    text = "${tier.label} · ${patient.riskScore()}",
    backgroundColor = bg,
    textColor = text,
    borderColor = border,
  )
}

@Composable
private fun CreateFollowUpDialog(
  patient: Patient,
  initialDueDays: Int,
  onConfirm: (reason: String, dueInDays: Int) -> Unit,
  onDismiss: () -> Unit,
) {
  var reason by remember { mutableStateOf("") }
  var dueDays by remember(patient.id, initialDueDays) { mutableIntStateOf(initialDueDays) }
  val dayOptions = listOf(7, 14, 30)

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = CardSurface,
    title = {
      Text(
        text = "Create follow-up for ${patient.firstName}",
        style = MaterialTheme.typography.titleLarge,
        color = HeadingDarkGreen,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = reason,
          onValueChange = { reason = it },
          label = { Text("Reason") },
          placeholder = { Text("e.g. Cholesterol recheck") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SidebarDarkGreen,
            unfocusedBorderColor = HairlineBorder,
            focusedLabelColor = HeadingDarkGreen,
            cursorColor = SidebarDarkGreen,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("followup_reason_input"),
        )
        Text(
          text = "Due in",
          style = MaterialTheme.typography.labelLarge,
          color = BodyInkPrimary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          dayOptions.forEach { days ->
            val selected = dueDays == days
            Surface(
              color = if (selected) SidebarDarkGreen else SoftGreenSurface,
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, if (selected) SidebarDarkGreen else HairlineBorder),
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { dueDays = days }
                .testTag("followup_due_${days}d"),
            ) {
              Text(
                text = "$days days",
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Color.White else HeadingDarkGreen,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              )
            }
          }
        }
      }
    },
    confirmButton = {
      CareLoopPrimaryButton(
        text = "Create follow-up",
        enabled = reason.trim().isNotEmpty(),
        onClick = { onConfirm(reason.trim(), dueDays) },
        modifier = Modifier.testTag("followup_dialog_submit"),
      )
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = BodyInkSecondary)
      }
    },
  )
}

@Composable
private fun DoctorAiPulseButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "ai_pulse")
  val scale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.07f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "ai_pulse_scale",
  )

  FloatingActionButton(
    onClick = onClick,
    shape = CircleShape,
    containerColor = SidebarDarkGreen,
    contentColor = Color.White,
    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
    modifier = modifier
      .size(62.dp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      },
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = Icons.Outlined.AutoAwesome,
        contentDescription = "CareLoop AI Assistant",
        modifier = Modifier.size(20.dp),
      )
      Text(
        text = "AI",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
      )
    }
  }
}

@Composable
private fun DoctorAiAssistantSheetContent(
  doctorId: DoctorId,
  messages: List<DoctorChatMessage>,
  isLoading: Boolean,
  onSend: (String) -> Unit,
  onClose: () -> Unit,
) {
  var question by remember { mutableStateOf("") }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 8.dp)
      .navigationBarsPadding()
      .padding(bottom = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column {
        Text(
          text = "CareLoop AI Assistant",
          style = MaterialTheme.typography.titleLarge,
          color = HeadingDarkGreen,
        )
        Text(
          text = "AI-generated, not medical advice · No medical notes sent",
          style = MaterialTheme.typography.labelSmall,
          color = BodyInkSecondary,
          modifier = Modifier.testTag("ai_assistant_disclaimer"),
        )
      }
      IconButton(onClick = onClose) {
        Icon(
          imageVector = Icons.Outlined.Close,
          contentDescription = "Close assistant",
          tint = BodyInkSecondary,
        )
      }
    }

    HorizontalDivider(color = HairlineBorder)

    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 180.dp, max = 300.dp)
        .testTag("ai_assistant_messages"),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (messages.isEmpty()) {
        item {
          Surface(
            color = SoftGreenSurface,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "Ask about ${doctorId.displayName}'s schedule, patient no-show risk, or urgent follow-up counts.",
              style = MaterialTheme.typography.bodySmall,
              color = HeadingDarkGreen,
              modifier = Modifier.padding(12.dp),
            )
          }
        }
      } else {
        items(messages, key = { it.id }) { msg ->
          val alignEnd = msg.isUser
          Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart,
          ) {
            Surface(
              color = when {
                msg.isError -> RiskHighBg
                msg.isUser -> SidebarDarkGreen
                else -> TableHeaderSurface
              },
              shape = RoundedCornerShape(12.dp),
              border = if (msg.isError) BorderStroke(1.dp, RiskHighBorder) else null,
            ) {
              Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                if (!msg.failureReason.isNullOrBlank()) {
                  Text(
                    text = msg.failureReason,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = RiskHighText,
                    modifier = Modifier.testTag("ai_assistant_failure_reason_${msg.id}"),
                  )
                }
                Text(
                  text = msg.text,
                  style = MaterialTheme.typography.bodyMedium,
                  color = when {
                    msg.isError -> RiskHighText
                    msg.isUser -> Color.White
                    else -> BodyInkPrimary
                  },
                )
                if (!msg.sourceLabel.isNullOrBlank()) {
                  Text(
                    text = msg.sourceLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = BodyInkSecondary,
                    modifier = Modifier.testTag("ai_assistant_source_label_${msg.id}"),
                  )
                }
              }
            }
          }
        }
      }
      if (isLoading) {
        item {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            CircularProgressIndicator(
              color = SidebarDarkGreen,
              strokeWidth = 2.dp,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = "Thinking...",
              style = MaterialTheme.typography.bodySmall,
              color = BodyInkSecondary,
            )
          }
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      OutlinedTextField(
        value = question,
        onValueChange = { question = it },
        placeholder = { Text("Ask about today's schedule or risk...") },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = SidebarDarkGreen,
          unfocusedBorderColor = HairlineBorder,
          cursorColor = SidebarDarkGreen,
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .weight(1f)
          .testTag("ai_assistant_input"),
      )
      IconButton(
        onClick = {
          val q = question.trim()
          if (q.isNotEmpty() && !isLoading) {
            onSend(q)
            question = ""
          }
        },
        enabled = question.trim().isNotEmpty() && !isLoading,
        modifier = Modifier.testTag("ai_assistant_send_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Outlined.Send,
          contentDescription = "Send",
          tint = HeadingDarkGreen,
        )
      }
    }
  }
}
