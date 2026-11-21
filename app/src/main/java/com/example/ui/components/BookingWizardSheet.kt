package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.model.ClinicSchedule
import com.example.model.ClinicSlot
import com.example.model.DoctorId
import com.example.model.MedicalSpecialization
import com.example.model.TimeOfDay
import com.example.ui.theme.BodyInkPrimary
import com.example.ui.theme.BodyInkSecondary
import com.example.ui.theme.CardSurface
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.HeadingDarkGreen
import com.example.ui.theme.RiskHighText
import com.example.ui.theme.SidebarDarkGreen
import com.example.ui.theme.SoftGreenSurface

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookingWizardSheet(
  initialDoctor: DoctorId? = null,
  initialReason: String = "",
  getFreeSlots: (DoctorId, String) -> List<ClinicSlot>,
  onOpenSettings: () -> Unit,
  onSubmit: (
    doctorId: DoctorId,
    dateKey: String,
    slotIndex: Int,
    reason: String,
    privateNote: String,
  ) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var step by remember {
    mutableIntStateOf(if (initialDoctor != null) 3 else 1)
  }
  var selectedSpec by remember {
    mutableStateOf(initialDoctor?.specialization)
  }
  var selectedDoctor by remember { mutableStateOf(initialDoctor) }
  var selectedDateKey by remember {
    mutableStateOf(
      ClinicSchedule.openDateKeysWithin(startOffsetDays = 1, daysWindow = 10).firstOrNull()
        ?: ClinicSchedule.todayLocalDateKey()
    )
  }
  var selectedSlotIndex by remember { mutableStateOf<Int?>(null) }
  var reason by remember { mutableStateOf(initialReason) }
  var privateNote by remember { mutableStateOf("") }
  var showDatePicker by remember { mutableStateOf(false) }

  if (showDatePicker) {
    ClinicDatePickerDialog(
      initialDateKey = selectedDateKey,
      onDateSelected = { newKey ->
        selectedDateKey = newKey
        selectedSlotIndex = null
        showDatePicker = false
      },
      onDismiss = { showDatePicker = false },
    )
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = CardSurface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .navigationBarsPadding()
        .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (step > 1) {
            IconButton(
              onClick = { step -= 1 },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = HeadingDarkGreen,
              )
            }
          }
          Column {
            Text(
              text = "Book an appointment",
              style = MaterialTheme.typography.titleLarge,
              color = HeadingDarkGreen,
            )
            Text(
              text = "Step $step of 5",
              style = MaterialTheme.typography.bodySmall,
              color = BodyInkSecondary,
            )
          }
        }
        IconButton(onClick = onDismiss) {
          Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = "Close",
            tint = BodyInkSecondary,
          )
        }
      }

      LinearProgressIndicator(
        progress = { step / 5f },
        color = SidebarDarkGreen,
        trackColor = SoftGreenSurface,
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(999.dp))
          .testTag("wizard_progress_bar"),
      )

      AnimatedContent(
        targetState = step,
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
        label = "wizard_step",
      ) { currentStep ->
        when (currentStep) {
          1 -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Text(
                text = "What do you need help with?",
                style = MaterialTheme.typography.titleMedium,
                color = HeadingDarkGreen,
              )
              MedicalSpecialization.entries.forEach { spec ->
                val isSelected = selectedSpec == spec
                Surface(
                  color = if (isSelected) SoftGreenSurface else Color.White,
                  shape = RoundedCornerShape(12.dp),
                  border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) SidebarDarkGreen else HairlineBorder,
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                      selectedSpec = spec
                      selectedDoctor = null
                      selectedSlotIndex = null
                      step = 2
                    }
                    .testTag("wizard_spec_${spec.name.lowercase()}"),
                ) {
                  Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.MedicalServices,
                      contentDescription = null,
                      tint = HeadingDarkGreen,
                      modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = spec.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = HeadingDarkGreen,
                      )
                      Text(
                        text = spec.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = BodyInkSecondary,
                      )
                    }
                  }
                }
              }
            }
          }

          2 -> {
            val doctorsInSpec = remember(selectedSpec) {
              if (selectedSpec == null) DoctorId.entries
              else DoctorId.entries.filter { it.specialization == selectedSpec }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Text(
                text = "Choose a doctor",
                style = MaterialTheme.typography.titleMedium,
                color = HeadingDarkGreen,
              )
              doctorsInSpec.forEach { doc ->
                val isSelected = selectedDoctor == doc
                Surface(
                  color = if (isSelected) SoftGreenSurface else Color.White,
                  shape = RoundedCornerShape(12.dp),
                  border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) SidebarDarkGreen else HairlineBorder,
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                      selectedDoctor = doc
                      selectedSlotIndex = null
                      step = 3
                    }
                    .testTag("wizard_doctor_${doc.name.lowercase()}"),
                ) {
                  Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    CareLoopAvatar(
                      name = doc.displayName,
                      gender = doc.gender,
                      size = 44.dp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = doc.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = HeadingDarkGreen,
                      )
                      Text(
                        text = doc.specializationLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = BodyInkSecondary,
                      )
                    }
                  }
                }
              }
            }
          }

          3 -> {
            val doc = selectedDoctor ?: DoctorId.ALI_KHAN
            val freeSlots = remember(doc, selectedDateKey) {
              getFreeSlots(doc, selectedDateKey)
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Text(
                text = "Choose date & time",
                style = MaterialTheme.typography.titleMedium,
                color = HeadingDarkGreen,
              )
              Text(
                text = "${doc.displayName} · ${doc.specializationLabel} (Mon–Sat, 9 AM–5 PM)",
                style = MaterialTheme.typography.bodySmall,
                color = BodyInkSecondary,
              )

              OutlinedButton(
                onClick = { showDatePicker = true },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier
                  .fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .testTag("wizard_date_button"),
              ) {
                Icon(
                  imageVector = Icons.Outlined.CalendarToday,
                  contentDescription = null,
                  tint = HeadingDarkGreen,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Date: ${ClinicSchedule.formatDateLabel(selectedDateKey)}",
                  style = MaterialTheme.typography.labelLarge,
                  color = HeadingDarkGreen,
                )
              }

              if (freeSlots.isEmpty()) {
                Text(
                  text = "No free slots on this date. Choose another day.",
                  style = MaterialTheme.typography.bodySmall,
                  color = RiskHighText,
                )
              } else {
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  freeSlots.forEach { slot ->
                    val isSelected = selectedSlotIndex == slot.index
                    Surface(
                      color = if (isSelected) SidebarDarkGreen else SoftGreenSurface,
                      shape = RoundedCornerShape(8.dp),
                      border = BorderStroke(1.dp, if (isSelected) SidebarDarkGreen else HairlineBorder),
                      modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedSlotIndex = slot.index }
                        .testTag("wizard_slot_${slot.index}"),
                    ) {
                      Text(
                        text = slot.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) Color.White else HeadingDarkGreen,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                      )
                    }
                  }
                }
              }

              CareLoopPrimaryButton(
                text = "Continue",
                enabled = selectedSlotIndex != null,
                onClick = { step = 4 },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("wizard_step3_next"),
              )
            }
          }

          4 -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Text(
                text = "Reason for visit",
                style = MaterialTheme.typography.titleMedium,
                color = HeadingDarkGreen,
              )
              OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Short reason") },
                placeholder = { Text("e.g. Cholesterol recheck or checkup") },
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = SidebarDarkGreen,
                  unfocusedBorderColor = HairlineBorder,
                  focusedLabelColor = HeadingDarkGreen,
                  cursorColor = SidebarDarkGreen,
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("wizard_reason_input"),
              )
              OutlinedTextField(
                value = privateNote,
                onValueChange = { privateNote = it },
                label = {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Outlined.Lock,
                      contentDescription = null,
                      modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Optional private note (encrypted)")
                  }
                },
                placeholder = { Text("Encrypted before saving; only your doctor can see") },
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = SidebarDarkGreen,
                  unfocusedBorderColor = HairlineBorder,
                  focusedLabelColor = HeadingDarkGreen,
                  cursorColor = SidebarDarkGreen,
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("wizard_private_note_input"),
              )

              CareLoopPrimaryButton(
                text = "Review",
                enabled = reason.trim().isNotEmpty(),
                onClick = { step = 5 },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("wizard_step4_next"),
              )
            }
          }

          5 -> {
            val doc = selectedDoctor ?: DoctorId.ALI_KHAN
            val slotIdx = selectedSlotIndex ?: 0
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Text(
                text = "Review request",
                style = MaterialTheme.typography.titleMedium,
                color = HeadingDarkGreen,
              )
              HorizontalDivider(color = HairlineBorder)
              ReviewRow("Doctor", "${doc.displayName} · ${doc.specializationLabel}")
              ReviewRow("Date & Time", ClinicSchedule.formatDateAndSlot(selectedDateKey, slotIdx))
              ReviewRow("Reason", reason.trim())
              if (privateNote.trim().isNotEmpty()) {
                ReviewRow("Private note", "Encrypted with AES-GCM")
              }
              HorizontalDivider(color = HairlineBorder)

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .clickable(onClick = onOpenSettings)
                  .padding(vertical = 6.dp)
                  .testTag("wizard_reminders_settings_link"),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Outlined.Settings,
                  contentDescription = null,
                  tint = HeadingDarkGreen,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Reminders follow your Settings",
                  style = MaterialTheme.typography.labelMedium.copy(
                    textDecoration = TextDecoration.Underline,
                  ),
                  color = HeadingDarkGreen,
                )
              }

              CareLoopPrimaryButton(
                text = "Submit request",
                icon = Icons.Outlined.CheckCircleOutline,
                onClick = {
                  onSubmit(doc, selectedDateKey, slotIdx, reason.trim(), privateNote.trim())
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("wizard_submit_button"),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ReviewRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = BodyInkSecondary,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
      color = BodyInkPrimary,
    )
  }
}
