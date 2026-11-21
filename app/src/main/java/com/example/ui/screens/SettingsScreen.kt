package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AppSettings
import com.example.model.CareLoopScreen
import com.example.model.ContactMethod
import com.example.model.InboxMessage
import com.example.model.LoggedInUser
import com.example.model.Patient
import com.example.model.ReceptionistQueueSort
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.components.CareLoopDashboardScaffold
import com.example.ui.components.CareLoopOutlinedButton
import com.example.ui.theme.BodyInkPrimary
import com.example.ui.theme.BodyInkSecondary
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.HeadingDarkGreen
import com.example.ui.theme.RiskHighText
import com.example.ui.theme.SidebarDarkGreen
import com.example.ui.theme.SoftGreenSurface
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.theme.SuccessGreenBorder
import com.example.ui.theme.SuccessGreenText
import com.example.ui.theme.TableHeaderSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  loggedInUser: LoggedInUser,
  userProfile: UserProfile?,
  patient: Patient?,
  appSettings: AppSettings,
  settingsSaved: Boolean,
  preferencesSaved: Boolean,
  currentScreen: CareLoopScreen.Settings,
  notifications: List<InboxMessage>,
  notificationsOpen: Boolean,
  topToastMessage: String?,
  onBack: () -> Unit,
  onNotificationSoundChange: (Boolean) -> Unit,
  onPopupAlertsChange: (Boolean) -> Unit,
  onReceptionistSortChange: (ReceptionistQueueSort) -> Unit,
  onReceptionistHighRiskFirstChange: (Boolean) -> Unit,
  onDoctorDefaultFollowUpDaysChange: (Int) -> Unit,
  onDoctorAiAssistantVisibleChange: (Boolean) -> Unit,
  onPreferredMethodChange: (ContactMethod) -> Unit,
  onSmsConsentChange: (Boolean) -> Unit,
  onEmailConsentChange: (Boolean) -> Unit,
  onOpenNotifications: () -> Unit,
  onCloseNotifications: () -> Unit,
  onAcknowledgeNotification: (Long) -> Unit,
  onMarkAllRead: () -> Unit,
  onNotificationClick: (InboxMessage) -> Unit = {},
  onDismissTopToast: () -> Unit,
  onOpenProfile: () -> Unit,
  onLogout: () -> Unit,
) {
  BackHandler(onBack = onBack)

  CareLoopDashboardScaffold(
    loggedInUser = loggedInUser,
    userProfile = userProfile,
    currentScreen = currentScreen,
    notifications = notifications,
    notificationsOpen = notificationsOpen,
    topToastMessage = topToastMessage,
    onOpenNotifications = onOpenNotifications,
    onCloseNotifications = onCloseNotifications,
    onAcknowledgeNotification = onAcknowledgeNotification,
    onMarkAllRead = onMarkAllRead,
    onNotificationClick = onNotificationClick,
    onDismissTopToast = onDismissTopToast,
    onOpenSettings = {},
    onOpenProfile = onOpenProfile,
    onLogout = onLogout,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 560.dp)
          .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          IconButton(
            onClick = onBack,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("settings_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = "Back",
              tint = HeadingDarkGreen,
            )
          }
          Spacer(modifier = Modifier.width(4.dp))
          Column {
            Text(
              text = "Settings",
              style = MaterialTheme.typography.headlineSmall,
              color = HeadingDarkGreen,
            )
            Text(
              text = when (loggedInUser.role) {
                UserRole.PATIENT -> "Alerts, reminders & consent"
                UserRole.RECEPTIONIST -> "Alerts & queue preferences"
                UserRole.DOCTOR -> "Alerts & clinical workflow"
                UserRole.ADMIN -> "Alerts & system preferences"
              },
              style = MaterialTheme.typography.bodySmall,
              color = BodyInkSecondary,
            )
          }
        }

        HorizontalDivider(color = HairlineBorder)

        Text(
          text = "Alerts",
          style = MaterialTheme.typography.titleMedium,
          color = HeadingDarkGreen,
        )

        PreferenceToggleRow(
          label = "Notification sound",
          helper = "Play a soft tone when a new alert arrives.",
          checked = appSettings.notificationSoundEnabled,
          onCheckedChange = onNotificationSoundChange,
          testTag = "notification_sound_switch",
        )

        HorizontalDivider(color = HairlineBorder)

        PreferenceToggleRow(
          label = "Pop-up alerts",
          helper = "Show an alert banner at the top of the screen.",
          checked = appSettings.popupAlertsEnabled,
          onCheckedChange = onPopupAlertsChange,
          testTag = "popup_alerts_switch",
        )

        HorizontalDivider(color = HairlineBorder)

        when (loggedInUser.role) {
          UserRole.RECEPTIONIST -> {
            Text(
              text = "Queue preferences",
              style = MaterialTheme.typography.titleMedium,
              color = HeadingDarkGreen,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "Default queue sort",
                style = MaterialTheme.typography.titleSmall,
                color = BodyInkPrimary,
              )
              SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = ReceptionistQueueSort.entries
                options.forEachIndexed { index, option ->
                  SegmentedButton(
                    selected = appSettings.receptionistQueueSort == option,
                    onClick = { onReceptionistSortChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(
                      index = index,
                      count = options.size,
                    ),
                    colors = SegmentedButtonDefaults.colors(
                      activeContainerColor = SoftGreenSurface,
                      activeContentColor = HeadingDarkGreen,
                      inactiveContainerColor = Color.White,
                      inactiveContentColor = BodyInkPrimary,
                      activeBorderColor = SidebarDarkGreen,
                      inactiveBorderColor = HairlineBorder,
                    ),
                    modifier = Modifier.testTag("receptionist_sort_${option.name.lowercase()}"),
                  ) {
                    Text(
                      text = option.label,
                      style = MaterialTheme.typography.labelLarge,
                    )
                  }
                }
              }
            }

            HorizontalDivider(color = HairlineBorder)

            PreferenceToggleRow(
              label = "Show only high-risk first",
              helper = "Pin high-risk patients to the top of the queue.",
              checked = appSettings.receptionistHighRiskFirst,
              onCheckedChange = onReceptionistHighRiskFirstChange,
              testTag = "receptionist_high_risk_first_switch",
            )

            HorizontalDivider(color = HairlineBorder)
          }

          UserRole.DOCTOR -> {
            Text(
              text = "Doctor workflow",
              style = MaterialTheme.typography.titleMedium,
              color = HeadingDarkGreen,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "Default follow-up due",
                style = MaterialTheme.typography.titleSmall,
                color = BodyInkPrimary,
              )
              val dayOptions = listOf(7, 14, 30)
              SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                dayOptions.forEachIndexed { index, days ->
                  SegmentedButton(
                    selected = appSettings.doctorDefaultFollowUpDays == days,
                    onClick = { onDoctorDefaultFollowUpDaysChange(days) },
                    shape = SegmentedButtonDefaults.itemShape(
                      index = index,
                      count = dayOptions.size,
                    ),
                    colors = SegmentedButtonDefaults.colors(
                      activeContainerColor = SoftGreenSurface,
                      activeContentColor = HeadingDarkGreen,
                      inactiveContainerColor = Color.White,
                      inactiveContentColor = BodyInkPrimary,
                      activeBorderColor = SidebarDarkGreen,
                      inactiveBorderColor = HairlineBorder,
                    ),
                    modifier = Modifier.testTag("doctor_followup_default_${days}d"),
                  ) {
                    Text(
                      text = "$days days",
                      style = MaterialTheme.typography.labelLarge,
                    )
                  }
                }
              }
            }

            HorizontalDivider(color = HairlineBorder)

            PreferenceToggleRow(
              label = "AI assistant",
              helper = "Show or hide the floating AI assistant button.",
              checked = appSettings.doctorAiAssistantVisible,
              onCheckedChange = onDoctorAiAssistantVisibleChange,
              testTag = "doctor_ai_assistant_switch",
            )

            HorizontalDivider(color = HairlineBorder)
          }

          UserRole.PATIENT -> {
            if (patient != null) {
              Text(
                text = "Reminder preferences",
                style = MaterialTheme.typography.titleMedium,
                color = HeadingDarkGreen,
              )

              PreferenceToggleRow(
                label = "Allow reminders by SMS",
                helper = "You can turn this off anytime.",
                checked = patient.smsConsent,
                onCheckedChange = onSmsConsentChange,
                testTag = "sms_consent_switch",
              )

              HorizontalDivider(color = HairlineBorder)

              PreferenceToggleRow(
                label = "Allow reminders by Email",
                helper = "You can turn this off anytime.",
                checked = patient.emailConsent,
                onCheckedChange = onEmailConsentChange,
                testTag = "email_consent_switch",
              )

              HorizontalDivider(color = HairlineBorder)

              val enabledMethods = buildList {
                if (patient.smsConsent) add(ContactMethod.SMS)
                if (patient.emailConsent) add(ContactMethod.EMAIL)
              }

              if (enabledMethods.isEmpty()) {
                Text(
                  text = "No reminders will be sent.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = RiskHighText,
                  modifier = Modifier.testTag("no_reminders_notice"),
                )
              } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Text(
                    text = "Preferred method",
                    style = MaterialTheme.typography.titleSmall,
                    color = BodyInkPrimary,
                  )
                  SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    enabledMethods.forEachIndexed { index, method ->
                      SegmentedButton(
                        selected = patient.preferredContactMethod == method,
                        onClick = { onPreferredMethodChange(method) },
                        shape = SegmentedButtonDefaults.itemShape(
                          index = index,
                          count = enabledMethods.size,
                        ),
                        colors = SegmentedButtonDefaults.colors(
                          activeContainerColor = SoftGreenSurface,
                          activeContentColor = HeadingDarkGreen,
                          inactiveContainerColor = Color.White,
                          inactiveContentColor = BodyInkPrimary,
                          activeBorderColor = SidebarDarkGreen,
                          inactiveBorderColor = HairlineBorder,
                        ),
                        modifier = Modifier.testTag("contact_method_${method.name.lowercase()}"),
                      ) {
                        Text(
                          text = method.label,
                          style = MaterialTheme.typography.labelLarge,
                        )
                      }
                    }
                  }
                }
              }

              HorizontalDivider(color = HairlineBorder)
            }
          }

          UserRole.ADMIN -> Unit
        }

        if (settingsSaved || preferencesSaved) {
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
                .testTag("preferences_saved_message"),
            )
          }
        }

        CareLoopOutlinedButton(
          text = "Logout",
          icon = Icons.AutoMirrored.Outlined.Logout,
          tint = RiskHighText,
          borderColor = RiskHighText,
          onClick = onLogout,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_logout_button"),
        )

        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(color = HairlineBorder)

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_about_section"),
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

        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

@Composable
private fun PreferenceToggleRow(
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
