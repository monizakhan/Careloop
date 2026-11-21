package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AiReminderDraft
import com.example.model.AppointmentRequest
import com.example.model.AppointmentRequestStatus
import com.example.model.AttendanceOutcome
import com.example.model.CareLoopScreen
import com.example.model.ClinicSchedule
import com.example.model.ClinicSlot
import com.example.model.DecryptedContact
import com.example.model.DoctorId
import com.example.model.InboxMessage
import com.example.model.LoggedInUser
import com.example.model.MissedFollowUpTask
import com.example.model.Patient
import com.example.model.ReceptionistQueueSort
import com.example.model.ReceptionistTab
import com.example.model.ReminderDeliveryStatus
import com.example.model.ReminderFeedback
import com.example.model.ReminderFeedbackType
import com.example.model.RiskTier
import com.example.model.TimeOfDay
import com.example.model.UserProfile
import com.example.ui.components.CareLoopAvatar
import com.example.ui.components.CareLoopDashboardScaffold
import com.example.ui.components.CareLoopOutlinedButton
import com.example.ui.components.CareLoopPrimaryButton
import com.example.ui.components.ClinicDatePickerDialog
import com.example.ui.components.DashboardCard
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
import com.example.ui.theme.SidebarDarkGreen
import com.example.ui.theme.SoftGreenSurface
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.theme.SuccessGreenBorder
import com.example.ui.theme.SuccessGreenText
import com.example.ui.theme.TableHeaderSurface

private enum class RiskFilterOption(val label: String, val tier: RiskTier?) {
  ALL("All risk levels", null),
  HIGH("High risk", RiskTier.HIGH),
  MEDIUM("Medium risk", RiskTier.MEDIUM),
  LOW("Low risk", RiskTier.LOW),
}

private data class SlotPickerTarget(
  val request: AppointmentRequest,
  val isOfferMode: Boolean,
)

@Composable
fun ReceptionistScreen(
  loggedInUser: LoggedInUser,
  userProfile: UserProfile?,
  activeTab: ReceptionistTab,
  patients: List<Patient>,
  decryptedContactsByPatientId: Map<Int, DecryptedContact>,
  appointmentRequests: List<AppointmentRequest>,
  missedFollowUpTasks: List<MissedFollowUpTask>,
  extraAppointmentsCount: Int,
  feedbackByPatientId: Map<Int, ReminderFeedback>,
  reminderStatusByPatientId: Map<Int, ReminderDeliveryStatus>,
  aiDraftsByPatientId: Map<Int, AiReminderDraft>,
  aiDraftLoadingPatientIds: Set<Int>,
  queueSort: ReceptionistQueueSort = ReceptionistQueueSort.RISK,
  highRiskFirst: Boolean = false,
  notifications: List<InboxMessage>,
  notificationsOpen: Boolean,
  topToastMessage: String?,
  accessDeniedMessage: String?,
  schedulingError: String?,
  getFreeSlots: (DoctorId, String, TimeOfDay?) -> List<ClinicSlot>,
  onSelectTab: (ReceptionistTab) -> Unit,
  onSendReminder: (Int) -> Unit,
  onDraftReminderWithAi: (Int) -> Unit,
  onStartEditAiDraft: (Int) -> Unit,
  onUpdateAiDraftText: (Int, String) -> Unit,
  onFinishEditAiDraft: (Int) -> Unit,
  onApproveAndSendAiDraft: (Int) -> Unit,
  onDiscardAiDraft: (Int) -> Unit,
  onSendMissedFollowUp: (Long) -> Unit,
  onBookSlot: (Long, String, Int) -> Unit,
  onOfferAnotherTime: (Long, String, Int) -> Unit,
  onDeclineRequest: (Long, String) -> Unit,
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
  var searchQuery by remember { mutableStateOf("") }
  var riskFilter by remember { mutableStateOf(RiskFilterOption.ALL) }
  var filterMenuOpen by remember { mutableStateOf(false) }
  var slotPickerTarget by remember { mutableStateOf<SlotPickerTarget?>(null) }
  var declineTarget by remember { mutableStateOf<AppointmentRequest?>(null) }

  slotPickerTarget?.let { target ->
    SlotSelectionDialog(
      request = target.request,
      isOfferMode = target.isOfferMode,
      getFreeSlots = getFreeSlots,
      onConfirmSlot = { dateKey, slotIndex ->
        if (target.isOfferMode) {
          onOfferAnotherTime(target.request.id, dateKey, slotIndex)
        } else {
          onBookSlot(target.request.id, dateKey, slotIndex)
        }
        slotPickerTarget = null
      },
      onDismiss = { slotPickerTarget = null },
    )
  }

  declineTarget?.let { request ->
    DeclineReasonDialog(
      patientName = request.patientName,
      onConfirmDecline = { reason ->
        onDeclineRequest(request.id, reason)
        declineTarget = null
      },
      onDismiss = { declineTarget = null },
    )
  }

  val sortedPatients = remember(patients, queueSort, highRiskFirst) {
    val baseComparator = when (queueSort) {
      ReceptionistQueueSort.RISK -> compareByDescending<Patient> { it.riskScore() }
        .thenBy { it.appointmentDateKey }
        .thenBy { it.appointmentSlotIndex }
      ReceptionistQueueSort.TIME -> compareBy<Patient> { it.appointmentDateKey }
        .thenBy { it.appointmentSlotIndex }
        .thenByDescending { it.riskScore() }
    }
    val finalComparator = if (highRiskFirst) {
      compareBy<Patient> { if (it.riskTier() == RiskTier.HIGH) 0 else 1 }
        .then(baseComparator)
    } else {
      baseComparator
    }
    patients.sortedWith(finalComparator)
  }

  val filteredPatients = remember(sortedPatients, searchQuery, riskFilter) {
    val q = searchQuery.trim().lowercase()
    sortedPatients.filter { p ->
      val matchesTier = riskFilter.tier == null || p.riskTier() == riskFilter.tier
      val matchesQuery = q.isEmpty() ||
        p.name.lowercase().contains(q) ||
        p.appointmentTimeLabel.lowercase().contains(q) ||
        p.appointmentDateKey.lowercase().contains(q)
      matchesTier && matchesQuery
    }
  }

  val highRiskCount = remember(patients) { patients.count { it.riskTier() == RiskTier.HIGH } }
  val urgentCount = remember(patients) { patients.count { it.isUrgent } }
  val totalAppointments = patients.count { !it.cancelled } + extraAppointmentsCount

  CareLoopDashboardScaffold(
    loggedInUser = loggedInUser,
    userProfile = userProfile,
    currentScreen = CareLoopScreen.Receptionist(activeTab),
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
    onSelectReceptionistTab = onSelectTab,
    onLogout = onLogout,
  ) {
    AnimatedContent(
      targetState = activeTab,
      transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
      label = "receptionist_tab_transition",
    ) { tab ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
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
                  .testTag("receptionist_scheduling_error"),
              )
              TextButton(onClick = onClearSchedulingError) {
                Text("Dismiss", color = RiskHighText)
              }
            }
          }
        }

        when (tab) {
          ReceptionistTab.QUEUE -> {
            // Plain text summary stats (no heavy card boxes)
            Text(
              text = "${patients.size} patients · $highRiskCount high risk · $urgentCount urgent · $totalAppointments total appointments",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
              color = BodyInkSecondary,
              modifier = Modifier.testTag("receptionist_plain_stats"),
            )

            // Search bar + risk filter dropdown above queue
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search patient or date") },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = BodyInkSecondary,
                    modifier = Modifier.size(18.dp),
                  )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = SidebarDarkGreen,
                  unfocusedBorderColor = HairlineBorder,
                  cursorColor = SidebarDarkGreen,
                  focusedContainerColor = CardSurface,
                  unfocusedContainerColor = CardSurface,
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("receptionist_search_input"),
              )

              Box {
                OutlinedButton(
                  onClick = { filterMenuOpen = true },
                  shape = RoundedCornerShape(10.dp),
                  border = BorderStroke(1.dp, HairlineBorder),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("receptionist_risk_filter_button"),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.FilterList,
                    contentDescription = "Filter by risk",
                    tint = HeadingDarkGreen,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = riskFilter.tier?.label ?: "All",
                    color = HeadingDarkGreen,
                    style = MaterialTheme.typography.labelLarge,
                  )
                }
                DropdownMenu(
                  expanded = filterMenuOpen,
                  onDismissRequest = { filterMenuOpen = false },
                ) {
                  RiskFilterOption.entries.forEach { option ->
                    DropdownMenuItem(
                      text = { Text(option.label) },
                      onClick = {
                        riskFilter = option
                        filterMenuOpen = false
                      },
                    )
                  }
                }
              }
            }

            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
              val useWideList = maxWidth >= 760.dp
              LazyColumn(
                modifier = Modifier
                  .fillMaxSize()
                  .testTag("receptionist_queue_list"),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(if (useWideList) 2.dp else 10.dp),
              ) {
                items(filteredPatients, key = { it.id }) { patient ->
                  ReceptionistPatientQueueItem(
                    patient = patient,
                    decryptedContact = decryptedContactsByPatientId[patient.id],
                    feedback = feedbackByPatientId[patient.id],
                    reminderStatus = reminderStatusByPatientId[patient.id],
                    aiDraft = aiDraftsByPatientId[patient.id],
                    isAiLoading = patient.id in aiDraftLoadingPatientIds,
                    defaultExpanded = false,
                    onSendReminder = { onSendReminder(patient.id) },
                    onDraftWithAi = { onDraftReminderWithAi(patient.id) },
                    onStartEditDraft = { onStartEditAiDraft(patient.id) },
                    onUpdateDraftText = { text -> onUpdateAiDraftText(patient.id, text) },
                    onFinishEditDraft = { onFinishEditAiDraft(patient.id) },
                    onApproveAndSendDraft = { onApproveAndSendAiDraft(patient.id) },
                    onDiscardDraft = { onDiscardAiDraft(patient.id) },
                  )
                }
              }
            }
          }

          ReceptionistTab.REQUESTS -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "Appointment Requests",
                style = MaterialTheme.typography.headlineSmall,
                color = HeadingDarkGreen,
              )
              Text(
                text = "Book into free slots, offer another time, or decline.",
                style = MaterialTheme.typography.bodySmall,
                color = BodyInkSecondary,
              )
            }
            HorizontalDivider(color = HairlineBorder)

            if (appointmentRequests.isEmpty()) {
              Text(
                text = "No patient appointment requests yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = BodyInkSecondary,
              )
            } else {
              LazyColumn(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxWidth()
                  .testTag("receptionist_requests_list"),
                contentPadding = PaddingValues(bottom = 16.dp),
              ) {
                items(appointmentRequests, key = { it.id }) { req ->
                  ReceptionistRequestRow(
                    request = req,
                    onBook = { slotPickerTarget = SlotPickerTarget(req, isOfferMode = false) },
                    onOfferAnotherTime = { slotPickerTarget = SlotPickerTarget(req, isOfferMode = true) },
                    onDecline = { declineTarget = req },
                  )
                  HorizontalDivider(color = HairlineBorder)
                }
              }
            }
          }

          ReceptionistTab.FOLLOW_UPS -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "Missed-Appointment Follow-ups",
                style = MaterialTheme.typography.headlineSmall,
                color = HeadingDarkGreen,
              )
              Text(
                text = "Patients marked as no-show require a consent-checked follow-up.",
                style = MaterialTheme.typography.bodySmall,
                color = BodyInkSecondary,
              )
            }
            HorizontalDivider(color = HairlineBorder)

            if (missedFollowUpTasks.isEmpty()) {
              Text(
                text = "No missed-appointment tasks right now.",
                style = MaterialTheme.typography.bodyMedium,
                color = BodyInkSecondary,
              )
            } else {
              LazyColumn(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxWidth()
                  .testTag("receptionist_followups_list"),
                contentPadding = PaddingValues(bottom = 16.dp),
              ) {
                items(missedFollowUpTasks, key = { it.id }) { task ->
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 10.dp)
                      .testTag("missed_followup_task_${task.id}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(
                      modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp),
                      verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                      Text(
                        text = task.patientName,
                        style = MaterialTheme.typography.titleSmall,
                        color = HeadingDarkGreen,
                      )
                      Text(
                        text = "Missed appointment on ${task.missedDateLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BodyInkSecondary,
                      )
                      task.feedback?.let { fb ->
                        Text(
                          text = fb.message,
                          style = MaterialTheme.typography.labelMedium,
                          color = if (fb.type == ReminderFeedbackType.SENT) SuccessGreenText else RiskHighText,
                        )
                      }
                    }
                    if (task.done) {
                      StatusPill(
                        text = "Done",
                        backgroundColor = SuccessGreenBg,
                        textColor = SuccessGreenText,
                        borderColor = SuccessGreenBorder,
                      )
                    } else {
                      CareLoopPrimaryButton(
                        text = "Send reminder",
                        onClick = { onSendMissedFollowUp(task.id) },
                        modifier = Modifier.testTag("missed_followup_send_${task.id}"),
                      )
                    }
                  }
                  HorizontalDivider(color = HairlineBorder)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ReceptionistPatientQueueItem(
  patient: Patient,
  decryptedContact: DecryptedContact?,
  feedback: ReminderFeedback?,
  reminderStatus: ReminderDeliveryStatus?,
  aiDraft: AiReminderDraft?,
  isAiLoading: Boolean,
  defaultExpanded: Boolean,
  onSendReminder: () -> Unit,
  onDraftWithAi: () -> Unit,
  onStartEditDraft: () -> Unit,
  onUpdateDraftText: (String) -> Unit,
  onFinishEditDraft: () -> Unit,
  onApproveAndSendDraft: () -> Unit,
  onDiscardDraft: () -> Unit,
) {
  var expanded by remember { mutableStateOf(defaultExpanded || aiDraft != null || feedback != null) }
  val tier = patient.riskTier()
  val (riskBg, riskText, riskBorder) = when (tier) {
    RiskTier.HIGH -> Triple(RiskHighBg, RiskHighText, RiskHighBorder)
    RiskTier.MEDIUM -> Triple(RiskMediumBg, RiskMediumText, RiskMediumBorder)
    RiskTier.LOW -> Triple(SuccessGreenBg, SuccessGreenText, SuccessGreenBorder)
  }

  DashboardCard(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("patient_queue_card_${patient.id}"),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { expanded = !expanded },
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CareLoopAvatar(
          name = patient.name,
          gender = patient.gender,
          size = 40.dp,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = patient.name,
            style = MaterialTheme.typography.titleSmall,
            color = HeadingDarkGreen,
          )
          Text(
            text = "${patient.assignedDoctor.nameWithSpecialization} · ${patient.appointmentTimeLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = BodyInkSecondary,
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        StatusPill(
          text = "${tier.label} (${patient.riskScore()})",
          backgroundColor = riskBg,
          textColor = riskText,
          borderColor = riskBorder,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
          imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
          contentDescription = if (expanded) "Collapse" else "Expand",
          tint = BodyInkSecondary,
        )
      }

      if (reminderStatus != null) {
        val (stBg, stText, stBorder) = when (reminderStatus) {
          ReminderDeliveryStatus.ACKNOWLEDGED -> Triple(SuccessGreenBg, SuccessGreenText, SuccessGreenBorder)
          ReminderDeliveryStatus.DELIVERED -> Triple(SoftGreenSurface, HeadingDarkGreen, HairlineBorder)
          ReminderDeliveryStatus.FAILED -> Triple(RiskHighBg, RiskHighText, RiskHighBorder)
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          StatusPill(
            text = "Reminder: ${reminderStatus.label}",
            backgroundColor = stBg,
            textColor = stText,
            borderColor = stBorder,
            modifier = Modifier.testTag("reminder_status_badge_${patient.id}"),
          )
        }
      }

      AnimatedVisibility(
        visible = expanded || aiDraft != null || feedback != null,
        enter = fadeIn(tween(180)) + expandVertically(tween(200)),
        exit = fadeOut(tween(150)) + shrinkVertically(tween(180)),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          HorizontalDivider(color = HairlineBorder)
          Text(
            text = patient.riskReasonSummary(),
            style = MaterialTheme.typography.bodySmall,
            color = BodyInkPrimary,
          )

          val contactLine = if (!patient.hasPhoneContact && !patient.hasEmailContact) {
            "Contact: Missing contact detail"
          } else if (decryptedContact != null) {
            val phonePart = decryptedContact.phone.ifBlank { "No phone" }
            val emailPart = decryptedContact.email.ifBlank { "No email" }
            "$phonePart · $emailPart"
          } else {
            "Encrypted contact"
          }
          Text(
            text = "$contactLine · Preferred: ${patient.preferredContactMethod.label} (SMS ${if (patient.smsConsent) "On" else "Off"}, Email ${if (patient.emailConsent) "On" else "Off"})",
            style = MaterialTheme.typography.bodySmall,
            color = if (!patient.hasPhoneContact && !patient.hasEmailContact) RiskHighText else BodyInkSecondary,
          )

          if (feedback != null) {
            val isOk = feedback.type == ReminderFeedbackType.SENT
            Surface(
              color = if (isOk) SuccessGreenBg else RiskHighBg,
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, if (isOk) SuccessGreenBorder else RiskHighBorder),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = feedback.message,
                style = MaterialTheme.typography.labelMedium,
                color = if (isOk) SuccessGreenText else RiskHighText,
                modifier = Modifier
                  .padding(horizontal = 12.dp, vertical = 8.dp)
                  .testTag("reminder_feedback_${patient.id}"),
              )
            }
          }

          if (aiDraft != null) {
            Surface(
              color = TableHeaderSurface,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, HairlineBorder),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_draft_panel_${patient.id}"),
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = aiDraft.statusNotice ?: "AI reminder draft — review before sending",
                  style = MaterialTheme.typography.labelMedium,
                  color = HeadingDarkGreen,
                )
                if (aiDraft.isEditing) {
                  OutlinedTextField(
                    value = aiDraft.text,
                    onValueChange = onUpdateDraftText,
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = SidebarDarkGreen,
                      unfocusedBorderColor = HairlineBorder,
                    ),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("ai_draft_input_${patient.id}"),
                  )
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CareLoopPrimaryButton(
                      text = "Done editing",
                      onClick = onFinishEditDraft,
                      modifier = Modifier.testTag("ai_draft_done_edit_${patient.id}"),
                    )
                  }
                } else {
                  Text(
                    text = aiDraft.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BodyInkPrimary,
                  )
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CareLoopPrimaryButton(
                      text = "Approve and send",
                      onClick = onApproveAndSendDraft,
                      modifier = Modifier.testTag("ai_draft_approve_${patient.id}"),
                    )
                    CareLoopOutlinedButton(
                      text = "Edit",
                      onClick = onStartEditDraft,
                      modifier = Modifier.testTag("ai_draft_edit_${patient.id}"),
                    )
                    TextButton(
                      onClick = onDiscardDraft,
                      modifier = Modifier.testTag("ai_draft_discard_${patient.id}"),
                    ) {
                      Text("Discard", color = RiskHighText)
                    }
                  }
                }
              }
            }
          } else {
            // Max 2 buttons visible per item
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              CareLoopPrimaryButton(
                text = stringResource(R.string.send_reminder),
                icon = Icons.Outlined.NotificationsActive,
                onClick = onSendReminder,
                modifier = Modifier.testTag("send_reminder_button_${patient.id}"),
              )
              CareLoopOutlinedButton(
                text = if (isAiLoading) "Drafting..." else "Draft with AI",
                icon = if (isAiLoading) null else Icons.Outlined.AutoAwesome,
                enabled = !isAiLoading,
                onClick = onDraftWithAi,
                modifier = Modifier.testTag("draft_ai_button_${patient.id}"),
              )
              if (isAiLoading) {
                CircularProgressIndicator(
                  color = SidebarDarkGreen,
                  strokeWidth = 2.dp,
                  modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.CenterVertically),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ReceptionistRequestRow(
  request: AppointmentRequest,
  onBook: () -> Unit,
  onOfferAnotherTime: () -> Unit,
  onDecline: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 12.dp)
      .testTag("receptionist_request_row_${request.id}"),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = request.patientName,
          style = MaterialTheme.typography.titleSmall,
          color = HeadingDarkGreen,
        )
        Text(
          text = request.doctorId.nameWithSpecialization,
          style = MaterialTheme.typography.bodySmall,
          color = BodyInkSecondary,
        )
      }
      val (bg, text, border) = when (request.status) {
        AppointmentRequestStatus.PENDING -> Triple(RiskMediumBg, RiskMediumText, RiskMediumBorder)
        AppointmentRequestStatus.BOOKED -> Triple(SuccessGreenBg, SuccessGreenText, SuccessGreenBorder)
        AppointmentRequestStatus.NEW_TIME_OFFERED -> Triple(SoftGreenSurface, HeadingDarkGreen, HairlineBorder)
        AppointmentRequestStatus.DECLINED -> Triple(RiskHighBg, RiskHighText, RiskHighBorder)
      }
      StatusPill(
        text = request.status.label,
        backgroundColor = bg,
        textColor = text,
        borderColor = border,
      )
    }

    val timeLabel = request.preferredSlotIndex?.let { ClinicSchedule.slotLabel(it) }
      ?: request.preferredTimeOfDay.label
    Text(
      text = "Requested: ${request.preferredDateLabel} · $timeLabel · ${request.reason}",
      style = MaterialTheme.typography.bodyMedium,
      color = BodyInkPrimary,
    )

    if (request.status == AppointmentRequestStatus.PENDING ||
      request.status == AppointmentRequestStatus.NEW_TIME_OFFERED
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CareLoopPrimaryButton(
          text = "Book",
          onClick = onBook,
          modifier = Modifier.testTag("request_book_button_${request.id}"),
        )
        CareLoopOutlinedButton(
          text = "Offer another time",
          onClick = onOfferAnotherTime,
          modifier = Modifier.testTag("request_offer_button_${request.id}"),
        )
        CareLoopOutlinedButton(
          text = "Decline",
          tint = RiskHighText,
          borderColor = RiskHighBorder,
          onClick = onDecline,
          modifier = Modifier.testTag("request_decline_button_${request.id}"),
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlotSelectionDialog(
  request: AppointmentRequest,
  isOfferMode: Boolean,
  getFreeSlots: (DoctorId, String, TimeOfDay?) -> List<ClinicSlot>,
  onConfirmSlot: (String, Int) -> Unit,
  onDismiss: () -> Unit,
) {
  var selectedDateKey by remember { mutableStateOf(request.preferredDateKey) }
  var selectedSlotIndex by remember { mutableStateOf(request.preferredSlotIndex) }
  var showDatePicker by remember { mutableStateOf(false) }

  val freeSlots = remember(request.doctorId, selectedDateKey) {
    getFreeSlots(request.doctorId, selectedDateKey, null)
  }

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
        text = if (isOfferMode) "Offer another time" else "Book appointment slot",
        style = MaterialTheme.typography.titleLarge,
        color = HeadingDarkGreen,
      )
    },
    text = {
      Column(
        modifier = Modifier
          .heightIn(max = 360.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "${request.patientName} · ${request.doctorId.nameWithSpecialization}",
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
            text = "Date: ${ClinicSchedule.formatDateLabel(selectedDateKey)}",
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
              val isSelected = selectedSlotIndex == slot.index
              Surface(
                color = if (isSelected) SidebarDarkGreen else SoftGreenSurface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (isSelected) SidebarDarkGreen else HairlineBorder),
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { selectedSlotIndex = slot.index },
              ) {
                Text(
                  text = slot.label,
                  style = MaterialTheme.typography.labelMedium,
                  color = if (isSelected) Color.White else HeadingDarkGreen,
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
        text = if (isOfferMode) "Send offer" else "Book",
        enabled = selectedSlotIndex != null,
        onClick = {
          val slot = selectedSlotIndex ?: return@CareLoopPrimaryButton
          onConfirmSlot(selectedDateKey, slot)
        },
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
private fun DeclineReasonDialog(
  patientName: String,
  onConfirmDecline: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var selectedReason by remember { mutableStateOf(ClinicSchedule.DECLINE_REASONS.first()) }
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = CardSurface,
    title = {
      Text(
        text = "Decline request for $patientName",
        style = MaterialTheme.typography.titleLarge,
        color = HeadingDarkGreen,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ClinicSchedule.DECLINE_REASONS.forEach { reason ->
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { selectedReason = reason },
          ) {
            RadioButton(
              selected = selectedReason == reason,
              onClick = { selectedReason = reason },
              colors = RadioButtonDefaults.colors(selectedColor = SidebarDarkGreen),
            )
            Text(
              text = reason,
              style = MaterialTheme.typography.bodyMedium,
              color = BodyInkPrimary,
            )
          }
        }
      }
    },
    confirmButton = {
      CareLoopPrimaryButton(
        text = "Confirm",
        onClick = { onConfirmDecline(selectedReason) },
      )
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = BodyInkSecondary)
      }
    },
  )
}
