package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DemoClinicData
import com.example.model.DoctorId
import com.example.model.Patient
import com.example.model.UserProfile
import com.example.ui.components.CareLoopLogo
import com.example.ui.components.DashboardCard
import com.example.ui.theme.BodyInkPrimary
import com.example.ui.theme.BodyInkSecondary
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.HeadingDarkGreen
import com.example.ui.theme.PageBackground
import com.example.ui.theme.RiskHighBg
import com.example.ui.theme.RiskHighBorder
import com.example.ui.theme.RiskHighText
import com.example.ui.theme.SidebarDarkGreen
import com.example.ui.theme.SidebarHoverGreen
import com.example.ui.theme.SoftGreenSurface

enum class LoginRoleCategory(
  val label: String,
  val icon: ImageVector,
) {
  RECEPTIONIST("Receptionist", Icons.Outlined.Badge),
  DOCTOR("Doctor", Icons.Outlined.MedicalServices),
  PATIENT("Patient", Icons.Outlined.Person),
  ADMIN("Admin", Icons.Outlined.AdminPanelSettings),
}

private data class LoginAccountOption(
  val name: String,
  val email: String,
  val subtitle: String? = null,
) {
  val displayLabel: String
    get() = if (subtitle != null) "$name · $subtitle ($email)" else "$name, $email"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
  patients: List<Patient>,
  userProfiles: Map<String, UserProfile>,
  loginError: String?,
  onEmailOrPasswordEdited: () -> Unit,
  onSignIn: (String, String) -> Unit,
) {
  var selectedRole by remember { mutableStateOf<LoginRoleCategory?>(null) }
  var selectedAccount by remember { mutableStateOf<LoginAccountOption?>(null) }
  var password by remember { mutableStateOf("") }
  var dropdownExpanded by remember { mutableStateOf(false) }

  val accountsForRole = remember(selectedRole, patients, userProfiles) {
    when (selectedRole) {
      LoginRoleCategory.RECEPTIONIST -> {
        val profile = userProfiles[DemoClinicData.ACCOUNT_RECEPTIONIST]
        listOf(
          LoginAccountOption(
            name = profile?.name ?: "Clinic Desk",
            email = profile?.email ?: DemoClinicData.RECEPTIONIST_EMAIL,
          )
        )
      }

      LoginRoleCategory.DOCTOR -> DoctorId.entries.map { doc ->
        val profile = userProfiles[DemoClinicData.doctorAccountKey(doc)]
        LoginAccountOption(
          name = profile?.name ?: doc.displayName,
          email = profile?.email ?: doc.defaultEmail,
          subtitle = doc.specializationLabel,
        )
      }

      LoginRoleCategory.PATIENT -> patients.map { patient ->
        val profile = userProfiles[DemoClinicData.patientAccountKey(patient.id)]
        val loginEmail = profile?.email?.takeIf { it.isNotBlank() } ?: patient.demoEmail
        LoginAccountOption(
          name = profile?.name ?: patient.name,
          email = loginEmail,
        )
      }

      LoginRoleCategory.ADMIN -> {
        val profile = userProfiles[DemoClinicData.ACCOUNT_ADMIN]
        listOf(
          LoginAccountOption(
            name = profile?.name ?: "Clinic Admin",
            email = profile?.email ?: DemoClinicData.ADMIN_EMAIL,
          )
        )
      }

      null -> emptyList()
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(PageBackground)
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 24.dp),
    contentAlignment = Alignment.Center,
  ) {
    DashboardCard(
      modifier = Modifier
        .widthIn(max = 460.dp)
        .fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          CareLoopLogo(
            iconSize = 36.dp,
            onDarkBackground = false,
          )
          Text(
            text = stringResource(R.string.login_tagline),
            style = MaterialTheme.typography.bodySmall,
            color = BodyInkSecondary,
            textAlign = TextAlign.Center,
          )
        }

        HorizontalDivider(color = HairlineBorder)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "1. Sign in as",
            style = MaterialTheme.typography.titleSmall,
            color = HeadingDarkGreen,
          )
          val selectRole: (LoginRoleCategory) -> Unit = { role ->
            if (selectedRole != role) {
              selectedRole = role
              selectedAccount = null
              password = ""
              dropdownExpanded = false
              onEmailOrPasswordEdited()
            }
          }
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              RoleOptionButton(
                role = LoginRoleCategory.RECEPTIONIST,
                selected = selectedRole == LoginRoleCategory.RECEPTIONIST,
                onClick = { selectRole(LoginRoleCategory.RECEPTIONIST) },
                modifier = Modifier
                  .weight(1f)
                  .testTag("login_role_receptionist"),
              )
              RoleOptionButton(
                role = LoginRoleCategory.DOCTOR,
                selected = selectedRole == LoginRoleCategory.DOCTOR,
                onClick = { selectRole(LoginRoleCategory.DOCTOR) },
                modifier = Modifier
                  .weight(1f)
                  .testTag("login_role_doctor"),
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              RoleOptionButton(
                role = LoginRoleCategory.PATIENT,
                selected = selectedRole == LoginRoleCategory.PATIENT,
                onClick = { selectRole(LoginRoleCategory.PATIENT) },
                modifier = Modifier
                  .weight(1f)
                  .testTag("login_role_patient"),
              )
              RoleOptionButton(
                role = LoginRoleCategory.ADMIN,
                selected = selectedRole == LoginRoleCategory.ADMIN,
                onClick = { selectRole(LoginRoleCategory.ADMIN) },
                modifier = Modifier
                  .weight(1f)
                  .testTag("login_role_admin"),
              )
            }
          }
        }

        AnimatedVisibility(
          visible = selectedRole != null,
          enter = fadeIn(tween(220)) + expandVertically(tween(220)),
          exit = fadeOut(tween(180)) + shrinkVertically(tween(180)),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = "2. Select your account",
              style = MaterialTheme.typography.titleSmall,
              color = HeadingDarkGreen,
            )

            ExposedDropdownMenuBox(
              expanded = dropdownExpanded,
              onExpandedChange = { dropdownExpanded = !dropdownExpanded },
              modifier = Modifier.fillMaxWidth(),
            ) {
              OutlinedTextField(
                value = selectedAccount?.displayLabel ?: "",
                onValueChange = {},
                readOnly = true,
                placeholder = {
                  Text(
                    text = "Choose an account",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BodyInkSecondary,
                  )
                },
                trailingIcon = {
                  ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = SidebarDarkGreen,
                  unfocusedBorderColor = HairlineBorder,
                  focusedContainerColor = Color.White,
                  unfocusedContainerColor = Color.White,
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                  .fillMaxWidth()
                  .testTag("login_account_dropdown"),
              )

              ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                modifier = Modifier.background(Color.White),
              ) {
                accountsForRole.forEachIndexed { index, account ->
                  DropdownMenuItem(
                    text = {
                      Column {
                        Text(
                          text = if (account.subtitle != null) "${account.name} · ${account.subtitle}" else account.name,
                          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                          color = BodyInkPrimary,
                        )
                        Text(
                          text = account.email,
                          style = MaterialTheme.typography.bodySmall,
                          color = BodyInkSecondary,
                        )
                      }
                    },
                    onClick = {
                      selectedAccount = account
                      password = DemoClinicData.DEMO_PASSWORD
                      dropdownExpanded = false
                      onEmailOrPasswordEdited()
                    },
                    modifier = Modifier.testTag("login_account_option_$index"),
                  )
                }
              }
            }
          }
        }

        AnimatedVisibility(
          visible = selectedAccount != null,
          enter = fadeIn(tween(220)) + expandVertically(tween(220)),
          exit = fadeOut(tween(180)) + shrinkVertically(tween(180)),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
              text = "3. Password",
              style = MaterialTheme.typography.titleSmall,
              color = HeadingDarkGreen,
            )

            OutlinedTextField(
              value = password,
              onValueChange = {
                password = it
                onEmailOrPasswordEdited()
              },
              label = { Text("Password") },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Outlined.Lock,
                  contentDescription = null,
                  tint = BodyInkSecondary,
                )
              },
              visualTransformation = PasswordVisualTransformation(),
              singleLine = true,
              keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
              ),
              keyboardActions = KeyboardActions(
                onDone = {
                  selectedAccount?.let { onSignIn(it.email, password) }
                },
              ),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SidebarDarkGreen,
                unfocusedBorderColor = HairlineBorder,
                focusedLabelColor = HeadingDarkGreen,
                cursorColor = SidebarDarkGreen,
              ),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("login_password_input"),
            )

            if (loginError != null) {
              Surface(
                color = RiskHighBg,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, RiskHighBorder),
                modifier = Modifier.fillMaxWidth(),
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
                    text = loginError,
                    style = MaterialTheme.typography.labelLarge,
                    color = RiskHighText,
                    modifier = Modifier.testTag("login_error_message"),
                  )
                }
              }
            }

            val signInInteraction = remember { MutableInteractionSource() }
            val isSignInHovered by signInInteraction.collectIsHoveredAsState()
            val signInBg by animateColorAsState(
              targetValue = if (isSignInHovered) SidebarHoverGreen else SidebarDarkGreen,
              animationSpec = tween(180),
              label = "sign_in_bg",
            )

            Button(
              onClick = {
                selectedAccount?.let { onSignIn(it.email, password) }
              },
              interactionSource = signInInteraction,
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = signInBg,
                contentColor = Color.White,
              ),
              contentPadding = PaddingValues(vertical = 14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("sign_in_button"),
            ) {
              Text(
                text = stringResource(R.string.sign_in),
                style = MaterialTheme.typography.labelLarge,
              )
            }
          }
        }

        HorizontalDivider(color = HairlineBorder)

        Text(
          text = "Demo password for all accounts: demo1234. Demo with fake data only.",
          style = MaterialTheme.typography.bodySmall,
          color = BodyInkSecondary,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }
}

@Composable
private fun RoleOptionButton(
  role: LoginRoleCategory,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isHovered by interactionSource.collectIsHoveredAsState()
  val backgroundColor by animateColorAsState(
    targetValue = when {
      selected -> SidebarDarkGreen
      isHovered -> SoftGreenSurface
      else -> Color.White
    },
    animationSpec = tween(180),
    label = "role_bg",
  )
  val borderColor by animateColorAsState(
    targetValue = if (selected) SidebarDarkGreen else HairlineBorder,
    animationSpec = tween(180),
    label = "role_border",
  )
  val contentColor = if (selected) Color.White else BodyInkPrimary

  Surface(
    color = backgroundColor,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, borderColor),
    modifier = modifier
      .minimumInteractiveComponentSize()
      .clip(RoundedCornerShape(12.dp))
      .clickable(
        interactionSource = interactionSource,
        indication = androidx.compose.material3.ripple(
          color = if (selected) Color.White else SidebarDarkGreen,
        ),
        onClick = onClick,
      ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = role.icon,
        contentDescription = null,
        tint = if (selected) Color.White else HeadingDarkGreen,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = role.label,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
