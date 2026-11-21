package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AdminTab
import com.example.model.AppSettings
import com.example.model.AppointmentRequest
import com.example.model.AppointmentRequestStatus
import com.example.model.AuditIntegrityResult
import com.example.model.AuditLogEntry
import com.example.model.AuditOutcome
import com.example.model.CareLoopScreen
import com.example.model.DoctorId
import com.example.model.InboxMessage
import com.example.model.LoggedInUser
import com.example.model.Patient
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.components.CareLoopAvatar
import com.example.ui.components.CareLoopDashboardScaffold
import com.example.ui.components.CareLoopOutlinedButton
import com.example.ui.components.CareLoopPrimaryButton
import com.example.ui.components.DashboardCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.BodyInkPrimary
import com.example.ui.theme.BodyInkSecondary
import com.example.ui.theme.CardSurface
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.HeadingDarkGreen
import com.example.ui.theme.RiskHighBg
import com.example.ui.theme.RiskHighBorder
import com.example.ui.theme.RiskHighText
import com.example.ui.theme.SidebarDarkGreen
import com.example.ui.theme.SoftGreenSurface
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.theme.SuccessGreenBorder
import com.example.ui.theme.SuccessGreenText
import com.example.ui.theme.TableHeaderSurface

@Composable
fun AdminScreen(
  loggedInUser: LoggedInUser,
  userProfile: UserProfile?,
  activeTab: AdminTab,
  patients: List<Patient>,
  appointmentRequests: List<AppointmentRequest>,
  bookedCount: Int,
  followUpCount: Int,
  auditLogs: List<AuditLogEntry>,
  auditIntegrityResult: AuditIntegrityResult?,
  appSettings: AppSettings = AppSettings(),
  settingsSaved: Boolean = false,
  notifications: List<InboxMessage>,
  notificationsOpen: Boolean,
  topToastMessage: String?,
  accessDeniedMessage: String?,
  onSelectTab: (AdminTab) -> Unit,
  onVerifyIntegrity: () -> Unit,
  onResetDemoData: () -> Unit,
  onNotificationSoundChange: (Boolean) -> Unit = {},
  onPopupAlertsChange: (Boolean) -> Unit = {},
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
  var showResetConfirm by remember { mutableStateOf(false) }

  if (showResetConfirm) {
    AlertDialog(
      onDismissRequest = { showResetConfirm = false },
      containerColor = CardSurface,
      title = {
        Text(
          text = "Reset demo data?",
          style = MaterialTheme.typography.titleLarge,
          color = HeadingDarkGreen,
        )
      },
      text = {
        Text(
          text = "This restores all patients, appointments, notifications, profiles, and the audit log back to initial demo defaults.",
          style = MaterialTheme.typography.bodyMedium,
          color = BodyInkPrimary,
        )
      },
      confirmButton = {
        CareLoopPrimaryButton(
          text = "Confirm",
          onClick = {
            showResetConfirm = false
            onResetDemoData()
          },
          modifier = Modifier.testTag("admin_reset_confirm_button"),
        )
      },
      dismissButton = {
        TextButton(onClick = { showResetConfirm = false }) {
          Text("Cancel", color = BodyInkSecondary)
        }
      },
    )
  }

  CareLoopDashboardScaffold(
    loggedInUser = loggedInUser,
    userProfile = userProfile,
    currentScreen = CareLoopScreen.Admin(activeTab),
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
    onSelectAdminTab = onSelectTab,
    onLogout = onLogout,
  ) {
    if (loggedInUser.role != UserRole.ADMIN) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(20.dp),
      ) {
        Surface(
          color = RiskHighBg,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, RiskHighBorder),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Outlined.ErrorOutline,
              contentDescription = null,
              tint = RiskHighText,
              modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Access denied",
              style = MaterialTheme.typography.titleMedium,
              color = RiskHighText,
              modifier = Modifier.testTag("admin_access_denied"),
            )
          }
        }
      }
      return@CareLoopDashboardScaffold
    }

    AnimatedContent(
      targetState = activeTab,
      transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
      label = "admin_tab_transition",
    ) { tab ->
      when (tab) {
        AdminTab.OVERVIEW -> AdminOverviewTab(
          patients = patients,
          appointmentRequests = appointmentRequests,
          bookedCount = bookedCount,
          followUpCount = followUpCount,
          auditLogs = auditLogs,
        )

        AdminTab.REPORTS -> AdminReportsTab(
          patients = patients,
          appointmentRequests = appointmentRequests,
          auditLogs = auditLogs,
        )

        AdminTab.AUDIT_LOG -> AdminAuditLogTab(
          entries = auditLogs,
          integrityResult = auditIntegrityResult,
          onVerifyIntegrity = onVerifyIntegrity,
        )

        AdminTab.SECURITY -> AdminSecurityTab(patients = patients)

        AdminTab.DOCTORS -> AdminDoctorsTab(patients = patients)

        AdminTab.SETTINGS -> AdminSettingsTab(
          patients = patients,
          appSettings = appSettings,
          settingsSaved = settingsSaved,
          onNotificationSoundChange = onNotificationSoundChange,
          onPopupAlertsChange = onPopupAlertsChange,
          onRequestReset = { showResetConfirm = true },
        )
      }
    }
  }
}

@Composable
private fun AdminOverviewTab(
  patients: List<Patient>,
  appointmentRequests: List<AppointmentRequest>,
  bookedCount: Int,
  followUpCount: Int,
  auditLogs: List<AuditLogEntry>,
) {
  val totalAppointments = patients.count { !it.cancelled } + bookedCount + followUpCount
  val totalMissed = patients.sumOf { it.pastMissedAppointments }
  val noShowRate = if (totalAppointments > 0) ((totalMissed * 100) / (totalAppointments + totalMissed)) else 0
  val blockedReminders = auditLogs.count { it.action.contains("blocked", ignoreCase = true) }
  val pendingRequests = appointmentRequests.count { it.status == AppointmentRequestStatus.PENDING }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 720.dp)
        .fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "Clinic Overview",
          style = MaterialTheme.typography.headlineSmall,
          color = HeadingDarkGreen,
          modifier = Modifier.testTag("admin_overview_heading"),
        )
        Text(
          text = "Operational metrics only. Patient medical notes stay encrypted.",
          style = MaterialTheme.typography.bodySmall,
          color = BodyInkSecondary,
        )
      }

      HorizontalDivider(color = HairlineBorder)

      MetricSummaryRow("Total active appointments", totalAppointments.toString())
      HorizontalDivider(color = HairlineBorder)
      MetricSummaryRow("Historical no-show rate", "$noShowRate%")
      HorizontalDivider(color = HairlineBorder)
      MetricSummaryRow("Blocked reminders (no consent)", blockedReminders.toString())
      HorizontalDivider(color = HairlineBorder)
      MetricSummaryRow("Pending appointment requests", pendingRequests.toString())
      HorizontalDivider(color = HairlineBorder)

      Text(
        text = "Recent audit events",
        style = MaterialTheme.typography.titleMedium,
        color = HeadingDarkGreen,
      )
      if (auditLogs.isEmpty()) {
        Text(
          text = "No audit entries recorded yet.",
          style = MaterialTheme.typography.bodySmall,
          color = BodyInkSecondary,
        )
      } else {
        auditLogs.take(5).forEach { entry ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = entry.action,
                style = MaterialTheme.typography.bodyMedium,
                color = BodyInkPrimary,
              )
              Text(
                text = "${entry.timeLabel} · ${entry.who}",
                style = MaterialTheme.typography.bodySmall,
                color = BodyInkSecondary,
              )
            }
            StatusPill(
              text = if (entry.outcome == AuditOutcome.ALLOWED) "Allowed" else "Denied",
              backgroundColor = if (entry.outcome == AuditOutcome.ALLOWED) SuccessGreenBg else RiskHighBg,
              textColor = if (entry.outcome == AuditOutcome.ALLOWED) SuccessGreenText else RiskHighText,
              borderColor = if (entry.outcome == AuditOutcome.ALLOWED) SuccessGreenBorder else RiskHighBorder,
            )
          }
          HorizontalDivider(color = HairlineBorder)
        }
      }
    }
  }
}

@Composable
private fun AdminReportsTab(
  patients: List<Patient>,
  appointmentRequests: List<AppointmentRequest>,
  auditLogs: List<AuditLogEntry>,
) {
  val totalMissed = patients.sumOf { it.pastMissedAppointments }
  val confirmedCount = patients.count { it.confirmed && !it.cancelled }
  val highRiskCount = patients.count { it.riskScore() >= 60 }
  val noShowsPerDoctor = remember(patients) {
    DoctorId.entries.map { doc ->
      doc to patients.filter { it.assignedDoctor == doc }.sumOf { it.pastMissedAppointments }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 720.dp)
        .fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "Reports",
          style = MaterialTheme.typography.headlineSmall,
          color = HeadingDarkGreen,
        )
        Text(
          text = "Simple appointment counts and no-shows by doctor.",
          style = MaterialTheme.typography.bodySmall,
          color = BodyInkSecondary,
        )
      }

      HorizontalDivider(color = HairlineBorder)
      MetricSummaryRow("Confirmed appointments", confirmedCount.toString())
      HorizontalDivider(color = HairlineBorder)
      MetricSummaryRow("High-risk patients", highRiskCount.toString())
      HorizontalDivider(color = HairlineBorder)
      MetricSummaryRow("Total missed appointments", totalMissed.toString())
      HorizontalDivider(color = HairlineBorder)
      MetricSummaryRow("Total requests logged", appointmentRequests.size.toString())
      HorizontalDivider(color = HairlineBorder)
      MetricSummaryRow("Audit log entries", auditLogs.size.toString())
      HorizontalDivider(color = HairlineBorder)

      Text(
        text = "No-shows per doctor",
        style = MaterialTheme.typography.titleMedium,
        color = HeadingDarkGreen,
      )

      NoShowsBarChart(
        data = noShowsPerDoctor,
        modifier = Modifier
          .fillMaxWidth()
          .height(220.dp)
          .testTag("reports_noshow_chart"),
      )
    }
  }
}

@Composable
private fun NoShowsBarChart(
  data: List<Pair<DoctorId, Int>>,
  modifier: Modifier = Modifier,
) {
  val maxVal = (data.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
  Surface(
    color = CardSurface,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, HairlineBorder),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
      ) {
        val barCount = data.size.coerceAtLeast(1)
        val slotWidth = size.width / barCount
        val barWidth = (slotWidth * 0.42f).coerceAtMost(56.dp.toPx())
        val topPadding = 24.dp.toPx()
        val chartHeight = (size.height - topPadding).coerceAtLeast(10f)

        data.forEachIndexed { index, (_, count) ->
          val ratio = count.toFloat() / maxVal.toFloat()
          val barHeight = (chartHeight * ratio).coerceAtLeast(if (count > 0) 8.dp.toPx() else 2.dp.toPx())
          val left = index * slotWidth + (slotWidth - barWidth) / 2f
          val top = size.height - barHeight

          drawRoundRect(
            color = SidebarDarkGreen,
            topLeft = Offset(left, top),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
          )

          drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
              color = android.graphics.Color.parseColor("#0F3D2E")
              textSize = 12.dp.toPx()
              textAlign = android.graphics.Paint.Align.CENTER
              isFakeBoldText = true
              isAntiAlias = true
            }
            drawText(
              count.toString(),
              left + barWidth / 2f,
              (top - 6.dp.toPx()).coerceAtLeast(12.dp.toPx()),
              paint,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        data.forEach { (doc, _) ->
          Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(
              text = doc.displayName.removePrefix("Dr. "),
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = BodyInkPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = doc.specializationLabel,
              style = MaterialTheme.typography.labelSmall,
              color = BodyInkSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AdminAuditLogTab(
  entries: List<AuditLogEntry>,
  integrityResult: AuditIntegrityResult?,
  onVerifyIntegrity: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp, vertical = 20.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(end = 10.dp),
      ) {
        Text(
          text = "Audit Log",
          style = MaterialTheme.typography.headlineSmall,
          color = HeadingDarkGreen,
        )
        Text(
          text = "SHA-256 tamper-proof chain (${entries.size} entries)",
          style = MaterialTheme.typography.bodySmall,
          color = BodyInkSecondary,
        )
      }
      CareLoopPrimaryButton(
        text = "Verify integrity",
        icon = Icons.Outlined.VerifiedUser,
        onClick = onVerifyIntegrity,
        modifier = Modifier.testTag("verify_integrity_button"),
      )
    }

    if (integrityResult != null) {
      val isOk = integrityResult.verified
      Surface(
        color = if (isOk) SuccessGreenBg else RiskHighBg,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isOk) SuccessGreenBorder else RiskHighBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = if (isOk) Icons.Outlined.CheckCircleOutline else Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = if (isOk) SuccessGreenText else RiskHighText,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = integrityResult.message,
            style = MaterialTheme.typography.labelLarge,
            color = if (isOk) SuccessGreenText else RiskHighText,
            modifier = Modifier.testTag("audit_integrity_message"),
          )
        }
      }
    }

    BoxWithConstraints(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
    ) {
      val useTableLayout = maxWidth >= 700.dp

      if (entries.isEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.ReceiptLong,
            contentDescription = null,
            tint = HeadingDarkGreen,
            modifier = Modifier.size(34.dp),
          )
          Text(
            text = "No actions recorded yet",
            style = MaterialTheme.typography.titleMedium,
            color = HeadingDarkGreen,
          )
          Text(
            text = "Logins, decryptions, reminders, requests, consents, and doctor decisions appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = BodyInkSecondary,
          )
        }
      } else if (useTableLayout) {
        DashboardCard(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("audit_log_table"),
        ) {
          LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
              AuditTableHeader()
              HorizontalDivider(color = HairlineBorder)
            }
            items(entries, key = { it.id }) { entry ->
              AuditTableRow(entry = entry)
              HorizontalDivider(color = HairlineBorder)
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .testTag("audit_log_cards"),
          contentPadding = PaddingValues(bottom = 12.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          items(entries, key = { it.id }) { entry ->
            AuditEntryRow(entry = entry)
            HorizontalDivider(color = HairlineBorder)
          }
        }
      }
    }
  }
}

@Composable
private fun AuditEntryRow(entry: AuditLogEntry) {
  val isAllowed = entry.outcome == AuditOutcome.ALLOWED
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 10.dp)
      .testTag("audit_row_${entry.id}"),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "#${entry.id} · ${entry.timeLabel} · ${entry.who} (${entry.role})",
        style = MaterialTheme.typography.labelMedium,
        color = BodyInkSecondary,
        modifier = Modifier
          .weight(1f)
          .padding(end = 8.dp),
      )
      StatusPill(
        text = if (isAllowed) "Allowed" else "Denied",
        backgroundColor = if (isAllowed) SuccessGreenBg else RiskHighBg,
        textColor = if (isAllowed) SuccessGreenText else RiskHighText,
        borderColor = if (isAllowed) SuccessGreenBorder else RiskHighBorder,
      )
    }
    Text(
      text = entry.action,
      style = MaterialTheme.typography.bodyMedium,
      color = BodyInkPrimary,
    )
    Text(
      text = "SHA-256: ${entry.hash.take(16)}…",
      style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
      color = BodyInkSecondary,
    )
  }
}

@Composable
private fun AuditTableHeader() {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(TableHeaderSurface)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text("Time", style = MaterialTheme.typography.labelMedium, color = HeadingDarkGreen, modifier = Modifier.weight(0.15f))
    Text("Who", style = MaterialTheme.typography.labelMedium, color = HeadingDarkGreen, modifier = Modifier.weight(0.18f))
    Text("Role", style = MaterialTheme.typography.labelMedium, color = HeadingDarkGreen, modifier = Modifier.weight(0.15f))
    Text("Action & Hash", style = MaterialTheme.typography.labelMedium, color = HeadingDarkGreen, modifier = Modifier.weight(0.38f))
    Text("Result", style = MaterialTheme.typography.labelMedium, color = HeadingDarkGreen, modifier = Modifier.weight(0.14f))
  }
}

@Composable
private fun AuditTableRow(entry: AuditLogEntry) {
  val isAllowed = entry.outcome == AuditOutcome.ALLOWED
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .testTag("audit_row_${entry.id}"),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = "#${entry.id} · ${entry.timeLabel}",
      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
      color = BodyInkSecondary,
      modifier = Modifier
        .weight(0.15f)
        .padding(end = 6.dp),
    )
    Text(
      text = entry.who,
      style = MaterialTheme.typography.bodyMedium,
      color = BodyInkPrimary,
      modifier = Modifier
        .weight(0.18f)
        .padding(end = 6.dp),
    )
    Text(
      text = entry.role,
      style = MaterialTheme.typography.bodyMedium,
      color = BodyInkSecondary,
      modifier = Modifier
        .weight(0.15f)
        .padding(end = 6.dp),
    )
    Column(
      modifier = Modifier
        .weight(0.38f)
        .padding(end = 6.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = entry.action,
        style = MaterialTheme.typography.bodyMedium,
        color = BodyInkPrimary,
      )
      Text(
        text = "SHA-256: ${entry.hash.take(16)}…",
        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
        color = BodyInkSecondary,
      )
    }
    Box(modifier = Modifier.weight(0.14f)) {
      StatusPill(
        text = if (isAllowed) "Allowed" else "Denied",
        backgroundColor = if (isAllowed) SuccessGreenBg else RiskHighBg,
        textColor = if (isAllowed) SuccessGreenText else RiskHighText,
        borderColor = if (isAllowed) SuccessGreenBorder else RiskHighBorder,
      )
    }
  }
}

@Composable
private fun AdminSecurityTab(patients: List<Patient>) {
  val sampleRecords = remember(patients) { patients.take(3) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("security_sample_list"),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Text(
        text = "Security",
        style = MaterialTheme.typography.headlineSmall,
        color = HeadingDarkGreen,
      )
    }
    item {
      Surface(
        color = SoftGreenSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = HeadingDarkGreen,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Data is encrypted with AES-GCM 256-bit. It is decrypted only after a role check passes. Admin never decrypts medical notes.",
            style = MaterialTheme.typography.bodyMedium,
            color = HeadingDarkGreen,
            modifier = Modifier.testTag("security_policy_notice"),
          )
        }
      }
    }

    items(sampleRecords, key = { it.id }) { patient ->
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp)
          .testTag("security_sample_card_${patient.id}"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "Record #${patient.id} · ${patient.name}",
            style = MaterialTheme.typography.titleSmall,
            color = HeadingDarkGreen,
          )
          StatusPill(
            text = "Encrypted with AES-GCM",
            backgroundColor = SuccessGreenBg,
            textColor = SuccessGreenText,
            borderColor = SuccessGreenBorder,
            modifier = Modifier.testTag("security_aes_badge_${patient.id}"),
          )
        }
        CipherRow("Medical note", patient.encryptedMedicalNote.orEmpty())
        CipherRow("Phone", patient.encryptedPhone.ifBlank { "(empty)" })
        CipherRow("Email", patient.encryptedEmail.ifBlank { "(empty)" })
        HorizontalDivider(color = HairlineBorder)
      }
    }
  }
}

@Composable
private fun CipherRow(label: String, ciphertext: String) {
  Surface(
    color = TableHeaderSurface,
    shape = RoundedCornerShape(8.dp),
    border = BorderStroke(1.dp, HairlineBorder),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = BodyInkSecondary,
      )
      Text(
        text = ciphertext,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        color = BodyInkPrimary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun AdminDoctorsTab(patients: List<Patient>) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 720.dp)
        .fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = "Doctors",
        style = MaterialTheme.typography.headlineSmall,
        color = HeadingDarkGreen,
      )
      Text(
        text = "Read-only roster and clinic working hours.",
        style = MaterialTheme.typography.bodySmall,
        color = BodyInkSecondary,
      )

      HorizontalDivider(color = HairlineBorder)

      DoctorId.entries.forEach { doc ->
        val assignedCount = patients.count { it.assignedDoctor == doc }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("admin_doctor_row_${doc.name.lowercase()}"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          CareLoopAvatar(
            name = doc.displayName,
            gender = doc.gender,
            size = 46.dp,
          )
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = doc.displayName,
              style = MaterialTheme.typography.titleSmall,
              color = HeadingDarkGreen,
            )
            Text(
              text = doc.specializationLabel,
              style = MaterialTheme.typography.bodyMedium,
              color = BodyInkPrimary,
            )
            Text(
              text = "Mon–Sat, 9:00 AM – 5:00 PM · $assignedCount patients",
              style = MaterialTheme.typography.bodySmall,
              color = BodyInkSecondary,
            )
          }
        }
        HorizontalDivider(color = HairlineBorder)
      }
    }
  }
}

@Composable
private fun AdminSettingsTab(
  patients: List<Patient>,
  appSettings: AppSettings,
  settingsSaved: Boolean,
  onNotificationSoundChange: (Boolean) -> Unit,
  onPopupAlertsChange: (Boolean) -> Unit,
  onRequestReset: () -> Unit,
) {
  val smsCount = patients.count { it.smsConsent }
  val emailCount = patients.count { it.emailConsent }
  val noConsentCount = patients.count { !it.smsConsent && !it.emailConsent }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 640.dp)
        .fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = "Admin Settings",
        style = MaterialTheme.typography.headlineSmall,
        color = HeadingDarkGreen,
      )

      HorizontalDivider(color = HairlineBorder)

      Text(
        text = "Alerts",
        style = MaterialTheme.typography.titleMedium,
        color = HeadingDarkGreen,
      )
      AdminToggleRow(
        label = "Notification sound",
        helper = "Play a soft tone when a new alert arrives.",
        checked = appSettings.notificationSoundEnabled,
        onCheckedChange = onNotificationSoundChange,
        testTag = "notification_sound_switch",
      )
      HorizontalDivider(color = HairlineBorder)
      AdminToggleRow(
        label = "Pop-up alerts",
        helper = "Show an alert banner at the top of the screen.",
        checked = appSettings.popupAlertsEnabled,
        onCheckedChange = onPopupAlertsChange,
        testTag = "popup_alerts_switch",
      )
      HorizontalDivider(color = HairlineBorder)

      if (settingsSaved) {
        Surface(
          color = SuccessGreenBg,
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, SuccessGreenBorder),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "Saved",
            style = MaterialTheme.typography.labelLarge,
            color = SuccessGreenText,
            modifier = Modifier
              .padding(horizontal = 12.dp, vertical = 8.dp)
              .testTag("admin_settings_saved_message"),
          )
        }
      }

      Text(
        text = "Consent summary",
        style = MaterialTheme.typography.titleMedium,
        color = HeadingDarkGreen,
      )
      Text(
        text = "Read-only consent summary across clinic patients.",
        style = MaterialTheme.typography.bodySmall,
        color = BodyInkSecondary,
      )

      HorizontalDivider(color = HairlineBorder)
      MetricSummaryRow("Patients with SMS consent", "$smsCount / ${patients.size}")
      HorizontalDivider(color = HairlineBorder)
      MetricSummaryRow("Patients with Email consent", "$emailCount / ${patients.size}")
      HorizontalDivider(color = HairlineBorder)
      MetricSummaryRow("Patients with no reminder consent", "$noConsentCount / ${patients.size}")
      HorizontalDivider(color = HairlineBorder)

      Spacer(modifier = Modifier.height(6.dp))

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Demo maintenance",
          style = MaterialTheme.typography.titleMedium,
          color = HeadingDarkGreen,
        )
        Text(
          text = "Reset all saved local data back to the original 15 demo patients and 4 doctors.",
          style = MaterialTheme.typography.bodySmall,
          color = BodyInkSecondary,
        )
        CareLoopOutlinedButton(
          text = "Reset demo data",
          icon = Icons.Outlined.RestartAlt,
          tint = RiskHighText,
          borderColor = RiskHighText,
          onClick = onRequestReset,
          modifier = Modifier.testTag("admin_reset_demo_button"),
        )
      }

      Spacer(modifier = Modifier.height(6.dp))
      HorizontalDivider(color = HairlineBorder)

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("admin_about_section"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = "About",
          style = MaterialTheme.typography.labelLarge,
          color = BodyInkSecondary,
        )
        Text(
          text = "Clinic schedule: Monday – Saturday, 9:00 AM – 5:00 PM (30-minute slots)",
          style = MaterialTheme.typography.bodySmall,
          color = BodyInkSecondary,
        )
        Text(
          text = "In-app notifications sync live across active sessions.",
          style = MaterialTheme.typography.bodySmall,
          color = BodyInkSecondary,
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@Composable
private fun AdminToggleRow(
  label: String,
  helper: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        color = BodyInkPrimary,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = helper,
        style = MaterialTheme.typography.bodySmall,
        color = BodyInkSecondary,
      )
    }
    Spacer(modifier = Modifier.width(12.dp))
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = SidebarDarkGreen,
        uncheckedThumbColor = BodyInkSecondary,
        uncheckedTrackColor = TableHeaderSurface,
        uncheckedBorderColor = HairlineBorder,
      ),
      modifier = Modifier
        .minimumInteractiveComponentSize()
        .testTag(testTag),
    )
  }
}

@Composable
private fun MetricSummaryRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = BodyInkSecondary,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
      color = HeadingDarkGreen,
    )
  }
}
