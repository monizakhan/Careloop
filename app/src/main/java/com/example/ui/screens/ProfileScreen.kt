package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AvatarGender
import com.example.model.CareLoopScreen
import com.example.model.InboxMessage
import com.example.model.LoggedInUser
import com.example.model.UserProfile
import com.example.ui.components.CareLoopAvatar
import com.example.ui.components.CareLoopDashboardScaffold
import com.example.ui.components.CareLoopOutlinedButton
import com.example.ui.components.CareLoopPrimaryButton
import com.example.ui.theme.BodyInkSecondary
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.HeadingDarkGreen
import com.example.ui.theme.RiskHighText
import com.example.ui.theme.SidebarDarkGreen
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.theme.SuccessGreenBorder
import com.example.ui.theme.SuccessGreenText
import java.io.ByteArrayOutputStream

@Composable
fun ProfileScreen(
  loggedInUser: LoggedInUser,
  userProfile: UserProfile?,
  currentScreen: CareLoopScreen.Profile,
  notifications: List<InboxMessage>,
  notificationsOpen: Boolean,
  topToastMessage: String?,
  profileSaved: Boolean,
  onBack: () -> Unit,
  onSaveProfile: (name: String, phone: String, email: String, photoDataUrl: String?) -> Unit,
  onProfileEdited: () -> Unit,
  onOpenNotifications: () -> Unit,
  onCloseNotifications: () -> Unit,
  onAcknowledgeNotification: (Long) -> Unit,
  onMarkAllRead: () -> Unit,
  onNotificationClick: (InboxMessage) -> Unit = {},
  onDismissTopToast: () -> Unit,
  onOpenSettings: () -> Unit,
  onLogout: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  var name by remember(userProfile) { mutableStateOf(userProfile?.name ?: loggedInUser.displayName) }
  var phone by remember(userProfile) { mutableStateOf(userProfile?.phone.orEmpty()) }
  var email by remember(userProfile) { mutableStateOf(userProfile?.email ?: loggedInUser.email) }
  var photoDataUrl by remember(userProfile) { mutableStateOf(userProfile?.photoDataUrl) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri: Uri? ->
    if (uri != null) {
      val dataUrl = encodeImageUriToDataUrl(context, uri)
      if (dataUrl != null) {
        photoDataUrl = dataUrl
        onProfileEdited()
      }
    }
  }

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
    onOpenSettings = onOpenSettings,
    onOpenProfile = {},
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
              .testTag("profile_back_button"),
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
              text = "Profile",
              style = MaterialTheme.typography.headlineSmall,
              color = HeadingDarkGreen,
            )
            Text(
              text = if (loggedInUser.doctorId != null) {
                "${loggedInUser.roleLabel} account"
              } else {
                loggedInUser.roleLabel
              },
              style = MaterialTheme.typography.bodySmall,
              color = BodyInkSecondary,
            )
          }
        }

        HorizontalDivider(color = HairlineBorder)

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          CareLoopAvatar(
            name = name,
            gender = userProfile?.gender ?: AvatarGender.FEMALE,
            photoDataUrl = photoDataUrl,
            size = 72.dp,
            modifier = Modifier.testTag("profile_avatar_preview"),
          )
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CareLoopOutlinedButton(
              text = "Change photo",
              icon = Icons.Outlined.PhotoCamera,
              onClick = {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              modifier = Modifier.testTag("profile_change_photo_button"),
            )
            if (photoDataUrl != null) {
              TextButton(
                onClick = {
                  photoDataUrl = null
                  onProfileEdited()
                },
              ) {
                Text(
                  text = "Use illustrated avatar",
                  color = BodyInkSecondary,
                  style = MaterialTheme.typography.labelMedium,
                )
              }
            }
          }
        }

        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it
            onProfileEdited()
          },
          label = { Text("Full name") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SidebarDarkGreen,
            unfocusedBorderColor = HairlineBorder,
            focusedLabelColor = HeadingDarkGreen,
            cursorColor = SidebarDarkGreen,
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_name_input"),
        )

        OutlinedTextField(
          value = phone,
          onValueChange = {
            phone = it
            onProfileEdited()
          },
          label = { Text("Phone number") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SidebarDarkGreen,
            unfocusedBorderColor = HairlineBorder,
            focusedLabelColor = HeadingDarkGreen,
            cursorColor = SidebarDarkGreen,
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_phone_input"),
        )

        OutlinedTextField(
          value = email,
          onValueChange = {
            email = it
            onProfileEdited()
          },
          label = { Text("Email (also used to sign in)") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SidebarDarkGreen,
            unfocusedBorderColor = HairlineBorder,
            focusedLabelColor = HeadingDarkGreen,
            cursorColor = SidebarDarkGreen,
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_email_input"),
        )

        if (profileSaved) {
          Surface(
            color = SuccessGreenBg,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, SuccessGreenBorder),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Outlined.CheckCircleOutline,
                contentDescription = null,
                tint = SuccessGreenText,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Profile saved.",
                style = MaterialTheme.typography.labelLarge,
                color = SuccessGreenText,
                modifier = Modifier.testTag("profile_saved_message"),
              )
            }
          }
        }

        CareLoopPrimaryButton(
          text = "Save profile",
          enabled = name.trim().isNotEmpty() && email.trim().isNotEmpty(),
          onClick = { onSaveProfile(name, phone, email, photoDataUrl) },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_save_button"),
        )

        HorizontalDivider(color = HairlineBorder)

        CareLoopOutlinedButton(
          text = "Logout",
          icon = Icons.AutoMirrored.Outlined.Logout,
          tint = RiskHighText,
          borderColor = RiskHighText,
          onClick = onLogout,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_logout_button"),
        )
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

private fun encodeImageUriToDataUrl(context: Context, uri: Uri): String? {
  return try {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return null
    val original = BitmapFactory.decodeStream(inputStream)
    inputStream.close()
    if (original == null) return null

    val maxDim = 256
    val scale = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height, 1f)
    val width = (original.width * scale).toInt().coerceAtLeast(1)
    val height = (original.height * scale).toInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(original, width, height, true)
    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 78, out)
    val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    "data:image/jpeg;base64,$b64"
  } catch (_: Exception) {
    null
  }
}
