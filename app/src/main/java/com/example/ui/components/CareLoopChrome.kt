package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AdminTab
import com.example.model.AvatarGender
import com.example.model.CareLoopScreen
import com.example.model.ClinicSchedule
import com.example.model.DoctorTab
import com.example.model.InboxMessage
import com.example.model.LoggedInUser
import com.example.model.NotificationCategory
import com.example.model.PatientTab
import com.example.model.ReceptionistTab
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.theme.BodyInkPrimary
import com.example.ui.theme.BodyInkSecondary
import com.example.ui.theme.CardSurface
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.HeadingDarkGreen
import com.example.ui.theme.InterFamily
import com.example.ui.theme.PageBackground
import com.example.ui.theme.RiskHighBg
import com.example.ui.theme.RiskHighBorder
import com.example.ui.theme.RiskHighText
import com.example.ui.theme.RiskMediumText
import com.example.ui.theme.SidebarActiveGreen
import com.example.ui.theme.SidebarDarkGreen
import com.example.ui.theme.SidebarHoverGreen
import com.example.ui.theme.SidebarMutedLight
import com.example.ui.theme.SidebarTextLight
import com.example.ui.theme.SoftGreenSurface
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.theme.SuccessGreenBorder
import com.example.ui.theme.SuccessGreenText
import kotlinx.coroutines.delay

data class RoleNavItem(
  val label: String,
  val icon: ImageVector,
  val selected: Boolean,
  val badgeCount: Int = 0,
  val testTag: String,
  val onClick: () -> Unit,
)

@Composable
fun CareLoopLogoMark(
  size: Dp = 34.dp,
  onDarkBackground: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val cornerRadius = (size.value * 0.25f).dp
  Box(
    modifier = modifier
      .size(size)
      .clip(RoundedCornerShape(cornerRadius))
      .background(SidebarDarkGreen)
      .then(
        if (onDarkBackground) {
          Modifier.border(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.28f),
            shape = RoundedCornerShape(cornerRadius),
          )
        } else {
          Modifier
        }
      ),
    contentAlignment = Alignment.Center,
  ) {
    Image(
      painter = painterResource(id = R.drawable.ic_careloop_mark_white),
      contentDescription = "CareLoop logo",
      contentScale = ContentScale.Fit,
      modifier = Modifier.size(size * 0.92f),
    )
  }
}

@Composable
fun CareLoopLogo(
  iconSize: Dp = 32.dp,
  onDarkBackground: Boolean = false,
  subtitle: String? = null,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    CareLoopLogoMark(
      size = iconSize,
      onDarkBackground = onDarkBackground,
    )
    Spacer(modifier = Modifier.width(10.dp))
    Column {
      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.titleLarge.copy(
          fontFamily = InterFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 19.sp,
        ),
        color = if (onDarkBackground) Color.White else HeadingDarkGreen,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = if (onDarkBackground) SidebarMutedLight else BodyInkSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.testTag("top_bar_doctor_subtitle"),
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareLoopDashboardScaffold(
  loggedInUser: LoggedInUser,
  userProfile: UserProfile?,
  currentScreen: CareLoopScreen,
  notifications: List<InboxMessage>,
  notificationsOpen: Boolean,
  topToastMessage: String?,
  accessDeniedMessage: String? = null,
  onOpenNotifications: () -> Unit,
  onCloseNotifications: () -> Unit,
  onAcknowledgeNotification: (Long) -> Unit,
  onMarkAllRead: () -> Unit,
  onNotificationClick: (InboxMessage) -> Unit = {},
  onDismissTopToast: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenProfile: () -> Unit,
  onSelectPatientTab: (PatientTab) -> Unit = {},
  onSelectReceptionistTab: (ReceptionistTab) -> Unit = {},
  onSelectDoctorTab: (DoctorTab) -> Unit = {},
  onSelectAdminTab: (AdminTab) -> Unit = {},
  onLogout: () -> Unit,
  floatingActionButton: @Composable () -> Unit = {},
  content: @Composable () -> Unit,
) {
  val unreadCount = remember(notifications) { notifications.count { !it.isRead } }

  LaunchedEffect(topToastMessage) {
    if (topToastMessage != null) {
      delay(3500)
      onDismissTopToast()
    }
  }

  if (notificationsOpen) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = onCloseNotifications,
      sheetState = sheetState,
      containerColor = CardSurface,
    ) {
      NotificationsPanelContent(
        notifications = notifications,
        onAcknowledge = onAcknowledgeNotification,
        onNotificationClick = onNotificationClick,
        onMarkAllRead = onMarkAllRead,
        onClose = onCloseNotifications,
      )
    }
  }

  val sidebarItems = buildRoleNavItems(
    user = loggedInUser,
    currentScreen = currentScreen,
    includeAdminSettings = true,
    onSelectPatientTab = onSelectPatientTab,
    onSelectReceptionistTab = onSelectReceptionistTab,
    onSelectDoctorTab = onSelectDoctorTab,
    onSelectAdminTab = onSelectAdminTab,
  )

  val bottomNavItems = buildRoleNavItems(
    user = loggedInUser,
    currentScreen = currentScreen,
    includeAdminSettings = false,
    onSelectPatientTab = onSelectPatientTab,
    onSelectReceptionistTab = onSelectReceptionistTab,
    onSelectDoctorTab = onSelectDoctorTab,
    onSelectAdminTab = onSelectAdminTab,
  )

  val doctorTopSubtitle = if (loggedInUser.role == UserRole.DOCTOR && loggedInUser.doctorId != null) {
    "${loggedInUser.displayName} · ${loggedInUser.doctorId.specializationLabel}"
  } else {
    null
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(PageBackground),
  ) {
    val isWideScreen = maxWidth >= 700.dp

    if (isWideScreen) {
      Row(modifier = Modifier.fillMaxSize()) {
        SidebarContent(
          navItems = sidebarItems,
          onLogout = onLogout,
          modifier = Modifier
            .width(236.dp)
            .fillMaxHeight(),
        )
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(PageBackground),
        ) {
          Column(modifier = Modifier.fillMaxSize()) {
            DashboardTopBar(
              loggedInUser = loggedInUser,
              userProfile = userProfile,
              doctorSubtitle = doctorTopSubtitle,
              unreadCount = unreadCount,
              onOpenNotifications = onOpenNotifications,
              onOpenSettings = onOpenSettings,
              onOpenProfile = onOpenProfile,
            )
            if (accessDeniedMessage != null) {
              AccessDeniedBanner(message = accessDeniedMessage)
            }
            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding(),
            ) {
              content()
            }
          }
          Box(
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(20.dp)
              .navigationBarsPadding(),
          ) {
            floatingActionButton()
          }
          TopNotificationToast(
            message = topToastMessage,
            onDismiss = onDismissTopToast,
            modifier = Modifier
              .align(Alignment.TopCenter)
              .statusBarsPadding()
              .padding(top = 8.dp, start = 16.dp, end = 16.dp),
          )
        }
      }
    } else {
      Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
          containerColor = PageBackground,
          contentWindowInsets = WindowInsets.safeDrawing,
          topBar = {
            Column {
              DashboardTopBar(
                loggedInUser = loggedInUser,
                userProfile = userProfile,
                doctorSubtitle = doctorTopSubtitle,
                unreadCount = unreadCount,
                onOpenNotifications = onOpenNotifications,
                onOpenSettings = onOpenSettings,
                onOpenProfile = onOpenProfile,
              )
              if (accessDeniedMessage != null) {
                AccessDeniedBanner(message = accessDeniedMessage)
              }
            }
          },
          bottomBar = {
            if (bottomNavItems.isNotEmpty() &&
              currentScreen !is CareLoopScreen.Profile &&
              currentScreen !is CareLoopScreen.Settings
            ) {
              CareLoopBottomNavBar(items = bottomNavItems)
            }
          },
          floatingActionButton = floatingActionButton,
        ) { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding),
          ) {
            content()
          }
        }

        TopNotificationToast(
          message = topToastMessage,
          onDismiss = onDismissTopToast,
          modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(top = 8.dp, start = 16.dp, end = 16.dp),
        )
      }
    }
  }
}

private fun buildRoleNavItems(
  user: LoggedInUser,
  currentScreen: CareLoopScreen,
  includeAdminSettings: Boolean,
  onSelectPatientTab: (PatientTab) -> Unit,
  onSelectReceptionistTab: (ReceptionistTab) -> Unit,
  onSelectDoctorTab: (DoctorTab) -> Unit,
  onSelectAdminTab: (AdminTab) -> Unit,
): List<RoleNavItem> {
  return when (user.role) {
    UserRole.PATIENT -> {
      val activeTab = (currentScreen as? CareLoopScreen.PatientPortal)?.tab
      listOf(
        RoleNavItem(
          label = PatientTab.HOME.label,
          icon = Icons.Outlined.Home,
          selected = activeTab == PatientTab.HOME,
          testTag = "nav_patient_home",
          onClick = { onSelectPatientTab(PatientTab.HOME) },
        ),
        RoleNavItem(
          label = PatientTab.APPOINTMENTS.label,
          icon = Icons.Outlined.CalendarMonth,
          selected = activeTab == PatientTab.APPOINTMENTS,
          testTag = "nav_patient_appointments",
          onClick = { onSelectPatientTab(PatientTab.APPOINTMENTS) },
        ),
        RoleNavItem(
          label = PatientTab.FOLLOW_UPS.label,
          icon = Icons.Outlined.EventRepeat,
          selected = activeTab == PatientTab.FOLLOW_UPS,
          testTag = "nav_patient_followups",
          onClick = { onSelectPatientTab(PatientTab.FOLLOW_UPS) },
        ),
      )
    }

    UserRole.RECEPTIONIST -> {
      val activeTab = (currentScreen as? CareLoopScreen.Receptionist)?.tab
      listOf(
        RoleNavItem(
          label = ReceptionistTab.QUEUE.label,
          icon = Icons.Outlined.Assessment,
          selected = activeTab == ReceptionistTab.QUEUE,
          testTag = "nav_receptionist_queue",
          onClick = { onSelectReceptionistTab(ReceptionistTab.QUEUE) },
        ),
        RoleNavItem(
          label = ReceptionistTab.REQUESTS.label,
          icon = Icons.Outlined.Inbox,
          selected = activeTab == ReceptionistTab.REQUESTS,
          testTag = "nav_receptionist_requests",
          onClick = { onSelectReceptionistTab(ReceptionistTab.REQUESTS) },
        ),
        RoleNavItem(
          label = ReceptionistTab.FOLLOW_UPS.label,
          icon = Icons.Outlined.EventRepeat,
          selected = activeTab == ReceptionistTab.FOLLOW_UPS,
          testTag = "nav_receptionist_followups",
          onClick = { onSelectReceptionistTab(ReceptionistTab.FOLLOW_UPS) },
        ),
      )
    }

    UserRole.DOCTOR -> {
      val activeTab = (currentScreen as? CareLoopScreen.Doctor)?.tab
      listOf(
        RoleNavItem(
          label = DoctorTab.TODAY.label,
          icon = Icons.Outlined.Today,
          selected = activeTab == DoctorTab.TODAY,
          testTag = "nav_doctor_today",
          onClick = { onSelectDoctorTab(DoctorTab.TODAY) },
        ),
        RoleNavItem(
          label = DoctorTab.NEEDS_DECISION.label,
          icon = Icons.Outlined.PriorityHigh,
          selected = activeTab == DoctorTab.NEEDS_DECISION,
          testTag = "nav_doctor_needs_decision",
          onClick = { onSelectDoctorTab(DoctorTab.NEEDS_DECISION) },
        ),
        RoleNavItem(
          label = DoctorTab.PATIENTS.label,
          icon = Icons.Outlined.PeopleOutline,
          selected = activeTab == DoctorTab.PATIENTS,
          testTag = "nav_doctor_patients",
          onClick = { onSelectDoctorTab(DoctorTab.PATIENTS) },
        ),
      )
    }

    UserRole.ADMIN -> {
      val activeTab = (currentScreen as? CareLoopScreen.Admin)?.tab
      val base = mutableListOf(
        RoleNavItem(
          label = AdminTab.OVERVIEW.label,
          icon = Icons.Outlined.Dashboard,
          selected = activeTab == AdminTab.OVERVIEW,
          testTag = "nav_admin_overview",
          onClick = { onSelectAdminTab(AdminTab.OVERVIEW) },
        ),
        RoleNavItem(
          label = AdminTab.REPORTS.label,
          icon = Icons.Outlined.BarChart,
          selected = activeTab == AdminTab.REPORTS,
          testTag = "nav_admin_reports",
          onClick = { onSelectAdminTab(AdminTab.REPORTS) },
        ),
        RoleNavItem(
          label = AdminTab.AUDIT_LOG.label,
          icon = Icons.Outlined.ReceiptLong,
          selected = activeTab == AdminTab.AUDIT_LOG,
          testTag = "nav_audit_log",
          onClick = { onSelectAdminTab(AdminTab.AUDIT_LOG) },
        ),
        RoleNavItem(
          label = AdminTab.SECURITY.label,
          icon = Icons.Outlined.Lock,
          selected = activeTab == AdminTab.SECURITY,
          testTag = "nav_admin_security",
          onClick = { onSelectAdminTab(AdminTab.SECURITY) },
        ),
        RoleNavItem(
          label = AdminTab.DOCTORS.label,
          icon = Icons.Outlined.MedicalServices,
          selected = activeTab == AdminTab.DOCTORS,
          testTag = "nav_admin_doctors",
          onClick = { onSelectAdminTab(AdminTab.DOCTORS) },
        ),
      )
      if (includeAdminSettings) {
        base += RoleNavItem(
          label = AdminTab.SETTINGS.label,
          icon = Icons.Outlined.Settings,
          selected = activeTab == AdminTab.SETTINGS,
          testTag = "nav_admin_settings",
          onClick = { onSelectAdminTab(AdminTab.SETTINGS) },
        )
      }
      base
    }
  }
}

@Composable
private fun CareLoopBottomNavBar(items: List<RoleNavItem>) {
  Surface(
    color = CardSurface,
    shadowElevation = 4.dp,
  ) {
    Column {
      HorizontalDivider(color = HairlineBorder)
      NavigationBar(
        containerColor = CardSurface,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.navigationBarsPadding(),
      ) {
        items.forEach { item ->
          NavigationBarItem(
            selected = item.selected,
            onClick = item.onClick,
            icon = {
              Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                modifier = Modifier.size(20.dp),
              )
            },
            label = {
              Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = SidebarDarkGreen,
              selectedTextColor = SidebarDarkGreen,
              indicatorColor = SoftGreenSurface,
              unselectedIconColor = BodyInkSecondary,
              unselectedTextColor = BodyInkSecondary,
            ),
            modifier = Modifier.testTag(item.testTag),
          )
        }
      }
    }
  }
}

@Composable
private fun DashboardTopBar(
  loggedInUser: LoggedInUser,
  userProfile: UserProfile?,
  doctorSubtitle: String?,
  unreadCount: Int,
  onOpenNotifications: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenProfile: () -> Unit,
) {
  Surface(
    color = CardSurface,
    shadowElevation = 1.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.statusBarsPadding()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CareLoopLogo(
          iconSize = 30.dp,
          onDarkBackground = false,
          subtitle = doctorSubtitle,
          modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 1. Bell with unread count badge
        Box {
          IconButton(
            onClick = onOpenNotifications,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("top_bar_bell_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.Notifications,
              contentDescription = "Notifications",
              tint = HeadingDarkGreen,
            )
          }
          if (unreadCount > 0) {
            Box(
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 6.dp, end = 6.dp)
                .size(18.dp)
                .background(SidebarDarkGreen, CircleShape)
                .testTag("top_bar_unread_badge"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                ),
                color = Color.White,
              )
            }
          }
        }

        // 2. Settings gear
        IconButton(
          onClick = onOpenSettings,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("top_bar_settings_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = "Settings",
            tint = HeadingDarkGreen,
          )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // 3. Round profile avatar
        Box(
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .clickable(onClick = onOpenProfile)
            .testTag("top_bar_avatar_button"),
          contentAlignment = Alignment.Center,
        ) {
          CareLoopAvatar(
            name = userProfile?.name ?: loggedInUser.displayName,
            gender = userProfile?.gender ?: AvatarGender.FEMALE,
            photoDataUrl = userProfile?.photoDataUrl,
            size = 36.dp,
          )
        }
      }
      HorizontalDivider(color = HairlineBorder)
    }
  }
}

@Composable
private fun SidebarContent(
  navItems: List<RoleNavItem>,
  onLogout: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .background(SidebarDarkGreen)
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(horizontal = 16.dp, vertical = 20.dp),
  ) {
    CareLoopLogo(
      iconSize = 32.dp,
      onDarkBackground = true,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
    )

    Spacer(modifier = Modifier.height(24.dp))

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      navItems.forEach { item ->
        val interactionSource = remember { MutableInteractionSource() }
        val isHovered by interactionSource.collectIsHoveredAsState()
        val bgColor by animateColorAsState(
          targetValue = when {
            item.selected -> SidebarActiveGreen
            isHovered -> SidebarHoverGreen
            else -> Color.Transparent
          },
          animationSpec = tween(180),
          label = "sidebar_item_bg",
        )
        val contentColor = if (item.selected) Color.White else SidebarMutedLight
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(
              interactionSource = interactionSource,
              indication = androidx.compose.material3.ripple(color = Color.White),
              onClick = item.onClick,
            )
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .testTag(item.testTag),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = item.label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    HorizontalDivider(color = SidebarActiveGreen)
    Spacer(modifier = Modifier.height(12.dp))

    val logoutInteraction = remember { MutableInteractionSource() }
    val isLogoutHovered by logoutInteraction.collectIsHoveredAsState()
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(10.dp))
        .background(if (isLogoutHovered) SidebarHoverGreen else Color.Transparent)
        .clickable(
          interactionSource = logoutInteraction,
          indication = androidx.compose.material3.ripple(color = Color.White),
          onClick = onLogout,
        )
        .padding(horizontal = 12.dp, vertical = 12.dp)
        .testTag("logout_button"),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Outlined.Logout,
        contentDescription = null,
        tint = SidebarTextLight,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.width(12.dp))
      Text(
        text = stringResource(R.string.logout),
        style = MaterialTheme.typography.labelLarge,
        color = SidebarTextLight,
      )
    }
  }
}

@Composable
private fun NotificationsPanelContent(
  notifications: List<InboxMessage>,
  onAcknowledge: (Long) -> Unit,
  onNotificationClick: (InboxMessage) -> Unit,
  onMarkAllRead: () -> Unit,
  onClose: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 8.dp)
      .navigationBarsPadding(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column {
        Text(
          text = "Notifications",
          style = MaterialTheme.typography.titleLarge,
          color = HeadingDarkGreen,
        )
        Text(
          text = "Newest updates first",
          style = MaterialTheme.typography.bodySmall,
          color = BodyInkSecondary,
        )
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (notifications.any { !it.isRead }) {
          TextButton(onClick = onMarkAllRead) {
            Text(
              text = "Mark read",
              color = HeadingDarkGreen,
              style = MaterialTheme.typography.labelMedium,
            )
          }
        }
        IconButton(onClick = onClose) {
          Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = "Close notifications",
            tint = BodyInkSecondary,
          )
        }
      }
    }

    HorizontalDivider(color = HairlineBorder)

    if (notifications.isEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.Notifications,
          contentDescription = null,
          tint = BodyInkSecondary,
          modifier = Modifier.size(28.dp),
        )
        Text(
          text = "No notifications yet",
          style = MaterialTheme.typography.titleSmall,
          color = BodyInkPrimary,
        )
        Text(
          text = "Reminders, appointment updates, and follow-up alerts appear here.",
          style = MaterialTheme.typography.bodySmall,
          color = BodyInkSecondary,
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .height(380.dp)
          .testTag("notifications_list"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(notifications, key = { it.id }) { msg ->
          val unreadBg = if (!msg.isRead) SoftGreenSurface else Color.White
          val (categoryIcon, categoryTint) = notificationCategoryVisuals(msg.category)
          Surface(
            color = unreadBg,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (!msg.isRead) SidebarDarkGreen.copy(alpha = 0.35f) else HairlineBorder),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable { onNotificationClick(msg) }
              .testTag("notification_item_${msg.id}"),
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.weight(1f),
                ) {
                  Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = categoryTint,
                    modifier = Modifier.size(18.dp),
                  )
                  if (!msg.isRead) {
                    Box(
                      modifier = Modifier
                        .size(7.dp)
                        .background(SidebarDarkGreen, CircleShape),
                    )
                  }
                  Text(
                    text = msg.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = HeadingDarkGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = msg.timestampLabel,
                  style = MaterialTheme.typography.labelSmall,
                  color = BodyInkSecondary,
                )
              }

              Text(
                text = msg.text,
                style = MaterialTheme.typography.bodyMedium,
                color = BodyInkPrimary,
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = msg.channelLabel,
                  style = MaterialTheme.typography.labelSmall,
                  color = BodyInkSecondary,
                )
                if (!msg.acknowledged) {
                  OutlinedButton(
                    onClick = { onAcknowledge(msg.id) },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, SidebarDarkGreen),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HeadingDarkGreen),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("got_it_button_${msg.id}"),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.CheckCircleOutline,
                      contentDescription = null,
                      modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "Got it",
                      style = MaterialTheme.typography.labelMedium,
                    )
                  }
                } else {
                  Text(
                    text = "Acknowledged",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = SuccessGreenText,
                    modifier = Modifier.testTag("notification_acknowledged_${msg.id}"),
                  )
                }
              }
            }
          }
        }
      }
    }
    Spacer(modifier = Modifier.height(12.dp))
  }
}

private fun notificationCategoryVisuals(category: NotificationCategory): Pair<ImageVector, Color> = when (category) {
  NotificationCategory.URGENT_DECISION -> Icons.Outlined.PriorityHigh to RiskHighText
  NotificationCategory.CONFIRMED -> Icons.Outlined.CheckCircleOutline to SuccessGreenText
  NotificationCategory.CANCELLED -> Icons.Outlined.Cancel to RiskHighText
  NotificationCategory.NEW_BOOKING -> Icons.Outlined.Today to HeadingDarkGreen
  NotificationCategory.TOMORROW_UNCONFIRMED -> Icons.Outlined.Schedule to RiskMediumText
  NotificationCategory.OFFER_RESPONSE -> Icons.Outlined.EventRepeat to HeadingDarkGreen
  NotificationCategory.MISSED_NO_SHOW -> Icons.Outlined.EventBusy to RiskHighText
  NotificationCategory.CONSENT_BLOCKED -> Icons.Outlined.Block to RiskMediumText
  NotificationCategory.DELIVERY_FAILED -> Icons.Outlined.ErrorOutline to RiskHighText
  NotificationCategory.CONSENT_OFF -> Icons.Outlined.NotificationsOff to RiskMediumText
  NotificationCategory.ACCESS_DENIED -> Icons.Outlined.Lock to RiskHighText
  NotificationCategory.DOUBLE_BOOKING_BLOCKED -> Icons.Outlined.Block to RiskHighText
  NotificationCategory.STALE_REQUEST -> Icons.Outlined.Schedule to RiskMediumText
  NotificationCategory.AUDIT_INTEGRITY -> Icons.Outlined.VerifiedUser to HeadingDarkGreen
  NotificationCategory.REMINDER, NotificationCategory.GENERAL -> Icons.Outlined.Notifications to SidebarDarkGreen
}

@Composable
private fun TopNotificationToast(
  message: String?,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AnimatedVisibility(
    visible = message != null,
    enter = fadeIn(tween(200)) + slideInVertically(tween(220)) { -it },
    exit = fadeOut(tween(180)) + slideOutVertically(tween(200)) { -it },
    modifier = modifier,
  ) {
    Surface(
      color = SidebarDarkGreen,
      shape = RoundedCornerShape(12.dp),
      shadowElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onDismiss)
        .testTag("top_notification_toast"),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Outlined.Notifications,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = message.orEmpty(),
          style = MaterialTheme.typography.bodySmall,
          color = Color.White,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun AccessDeniedBanner(message: String) {
  Surface(
    color = RiskHighBg,
    border = BorderStroke(1.dp, RiskHighBorder),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    shape = RoundedCornerShape(10.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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
        text = message,
        style = MaterialTheme.typography.labelLarge,
        color = RiskHighText,
        modifier = Modifier.testTag("access_denied_banner"),
      )
    }
  }
}

@Composable
fun CareLoopPrimaryButton(
  text: String,
  onClick: () -> Unit,
  enabled: Boolean = true,
  icon: ImageVector? = null,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isHovered by interactionSource.collectIsHoveredAsState()
  val bg by animateColorAsState(
    targetValue = if (isHovered && enabled) SidebarHoverGreen else SidebarDarkGreen,
    animationSpec = tween(180),
    label = "primary_btn_hover",
  )
  Button(
    onClick = onClick,
    enabled = enabled,
    interactionSource = interactionSource,
    shape = RoundedCornerShape(10.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = bg,
      contentColor = Color.White,
    ),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    modifier = modifier.minimumInteractiveComponentSize(),
  ) {
    if (icon != null) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
    }
    Text(
      text = text,
      style = MaterialTheme.typography.labelLarge,
    )
  }
}

@Composable
fun CareLoopOutlinedButton(
  text: String,
  onClick: () -> Unit,
  enabled: Boolean = true,
  tint: Color = HeadingDarkGreen,
  borderColor: Color = SidebarDarkGreen,
  icon: ImageVector? = null,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isHovered by interactionSource.collectIsHoveredAsState()
  val bg by animateColorAsState(
    targetValue = if (isHovered && enabled) SoftGreenSurface else Color.Transparent,
    animationSpec = tween(180),
    label = "outlined_btn_hover",
  )
  OutlinedButton(
    onClick = onClick,
    enabled = enabled,
    interactionSource = interactionSource,
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, if (enabled) borderColor else HairlineBorder),
    colors = ButtonDefaults.outlinedButtonColors(
      containerColor = bg,
      contentColor = tint,
    ),
    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    modifier = modifier.minimumInteractiveComponentSize(),
  ) {
    if (icon != null) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(17.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
    }
    Text(
      text = text,
      style = MaterialTheme.typography.labelLarge,
    )
  }
}

@Composable
fun DashboardCard(
  modifier: Modifier = Modifier,
  borderColor: Color = HairlineBorder,
  content: @Composable ColumnScope.() -> Unit,
) {
  Surface(
    color = CardSurface,
    shape = RoundedCornerShape(14.dp),
    shadowElevation = 2.dp,
    border = BorderStroke(1.dp, borderColor),
    modifier = modifier,
  ) {
    Column(content = content)
  }
}

@Composable
fun StatusPill(
  text: String,
  backgroundColor: Color,
  textColor: Color,
  borderColor: Color = backgroundColor,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .background(color = backgroundColor, shape = RoundedCornerShape(999.dp))
      .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(999.dp))
      .padding(horizontal = 10.dp, vertical = 4.dp),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
      color = textColor,
      maxLines = 1,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicDatePickerDialog(
  initialDateKey: String?,
  onDateSelected: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val initialMillis = remember(initialDateKey) {
    if (!initialDateKey.isNullOrBlank()) {
      ClinicSchedule.utcMillisFromDateKey(initialDateKey)
    } else {
      val openDates = ClinicSchedule.openDateKeysWithin(startOffsetDays = 0, daysWindow = 7)
      openDates.firstOrNull()?.let { ClinicSchedule.utcMillisFromDateKey(it) }
    }
  }

  val datePickerState = androidx.compose.material3.rememberDatePickerState(
    initialSelectedDateMillis = initialMillis,
    selectableDates = object : androidx.compose.material3.SelectableDates {
      override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        ClinicSchedule.isSelectableClinicUtcDate(utcTimeMillis)

      override fun isSelectableYear(year: Int): Boolean {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        return year >= currentYear
      }
    },
  )

  androidx.compose.material3.DatePickerDialog(
    onDismissRequest = onDismiss,
    colors = androidx.compose.material3.DatePickerDefaults.colors(
      containerColor = CardSurface,
    ),
    confirmButton = {
      TextButton(
        onClick = {
          val millis = datePickerState.selectedDateMillis
          if (millis != null && ClinicSchedule.isSelectableClinicUtcDate(millis)) {
            onDateSelected(ClinicSchedule.dateKeyFromUtcMillis(millis))
          }
        },
        enabled = datePickerState.selectedDateMillis?.let {
          ClinicSchedule.isSelectableClinicUtcDate(it)
        } == true,
      ) {
        Text(
          text = "Select",
          color = HeadingDarkGreen,
          style = MaterialTheme.typography.labelLarge,
        )
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(
          text = "Cancel",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.labelLarge,
        )
      }
    },
  ) {
    androidx.compose.material3.DatePicker(
      state = datePickerState,
      colors = androidx.compose.material3.DatePickerDefaults.colors(
        containerColor = CardSurface,
        titleContentColor = HeadingDarkGreen,
        headlineContentColor = HeadingDarkGreen,
        weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        subheadContentColor = HeadingDarkGreen,
        navigationContentColor = HeadingDarkGreen,
        yearContentColor = MaterialTheme.colorScheme.onSurface,
        currentYearContentColor = HeadingDarkGreen,
        selectedYearContentColor = Color.White,
        selectedYearContainerColor = SidebarDarkGreen,
        dayContentColor = MaterialTheme.colorScheme.onSurface,
        disabledDayContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
        selectedDayContentColor = Color.White,
        selectedDayContainerColor = SidebarDarkGreen,
        todayContentColor = HeadingDarkGreen,
        todayDateBorderColor = SidebarDarkGreen,
      ),
    )
  }
}
