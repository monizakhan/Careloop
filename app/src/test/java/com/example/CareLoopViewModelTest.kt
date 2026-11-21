package com.example

import com.example.ai.AiTextResponse
import com.example.ai.CareLoopAiGateway
import com.example.ai.ClinicBackupAssistant
import com.example.ai.DoctorAssistantContext
import com.example.ai.FirebaseCareLoopAiGateway
import com.example.ai.HighRiskSummaryPatient
import com.example.ai.ReminderDraftRequest
import com.example.audio.CareLoopSoundPlayer
import com.example.data.CareLoopStorage
import com.example.data.DemoClinicData
import com.example.data.PersistedCareLoopSnapshot
import com.example.model.AdminTab
import com.example.model.AppointmentRequestStatus
import com.example.model.AttendanceOutcome
import com.example.model.AuditOutcome
import com.example.model.CareLoopScreen
import com.example.model.ClinicSchedule
import com.example.model.ContactMethod
import com.example.model.DoctorDecision
import com.example.model.DoctorId
import com.example.model.DoctorTab
import com.example.model.NotificationCategory
import com.example.model.ReceptionistQueueSort
import com.example.model.ReminderDeliveryStatus
import com.example.model.ReminderFeedbackType
import com.example.model.RiskTier
import com.example.model.TimeOfDay
import com.example.model.UserRole
import com.example.security.CareLoopCrypto
import com.example.viewmodel.CareLoopViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CareLoopViewModelTest {
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setUpDispatcher() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDownDispatcher() {
    Dispatchers.resetMain()
  }

  @Test
  fun `fake data has 15 patients, 4 doctors, 3 with no consent, 1 missing contact, and 2 urgent`() {
    val vm = CareLoopViewModel()
    val patients = vm.uiState.value.patients

    assertEquals(15, patients.size)
    assertEquals("Ayesha Malik", patients.first().name)
    assertEquals(DoctorId.ALI_KHAN, patients.first().assignedDoctor)
    assertEquals("ayesha.malik@careloop.demo", patients.first().demoEmail)
    assertEquals(3, patients.count { !it.smsConsent && !it.emailConsent })
    assertEquals(1, patients.count { !it.hasPhoneContact && !it.hasEmailContact })

    val urgentPatients = patients.filter { it.isUrgent }
    assertEquals(2, urgentPatients.size)
    assertTrue(urgentPatients.all { !it.encryptedMedicalNote.isNullOrBlank() })
    assertTrue(patients.none { it.encryptedMedicalNote?.contains("blood", ignoreCase = true) == true })
    assertTrue(patients.none { it.encryptedEmail.contains("@careloop.demo") })
    assertTrue(patients.none { it.encryptedPhone.contains("+92") })

    // 6 staff (Receptionist, 4 Doctors, Admin) + 15 patient demo accounts
    assertEquals(21, vm.uiState.value.demoAccounts.size)
    assertEquals(DoctorId.SANA_RAUF, patients.first { it.id == 2 }.assignedDoctor)
  }

  @Test
  fun `risk score and tiers match clinic formula`() {
    val vm = CareLoopViewModel()
    val ayesha = vm.uiState.value.patients.first { it.id == 1 }
    // 2 missed (*30 = 60) + not confirmed (+20) + urgent (+25) + within 48h (+10) = 115
    assertEquals(115, ayesha.riskScore())
    assertEquals(RiskTier.HIGH, ayesha.riskTier())
  }

  @Test
  fun `admin login opens overview and non-admins are denied admin access`() {
    val vm = CareLoopViewModel()
    assertTrue(vm.uiState.value.showSplash)
    vm.finishSplash()
    assertFalse(vm.uiState.value.showSplash)

    // Receptionist denied access to Audit Log
    vm.signIn(DemoClinicData.RECEPTIONIST_EMAIL, DemoClinicData.DEMO_PASSWORD)
    assertEquals(CareLoopScreen.Receptionist(), vm.uiState.value.currentScreen)
    vm.openAuditLog()
    assertEquals(CareLoopScreen.Receptionist(), vm.uiState.value.currentScreen)
    assertEquals("Access denied", vm.uiState.value.accessDeniedMessage)
    assertEquals(AuditOutcome.DENIED, vm.uiState.value.auditLogs.first().outcome)

    // Doctor denied access to Audit Log
    vm.signIn(DoctorId.ALI_KHAN.defaultEmail, DemoClinicData.DEMO_PASSWORD)
    assertEquals(CareLoopScreen.Doctor(DoctorId.ALI_KHAN), vm.uiState.value.currentScreen)
    vm.openAuditLog()
    assertEquals(CareLoopScreen.Doctor(DoctorId.ALI_KHAN), vm.uiState.value.currentScreen)
    assertEquals("Access denied", vm.uiState.value.accessDeniedMessage)
    assertEquals(AuditOutcome.DENIED, vm.uiState.value.auditLogs.first().outcome)

    // Admin signs in to Admin Overview
    vm.signIn(DemoClinicData.ADMIN_EMAIL, DemoClinicData.DEMO_PASSWORD)
    assertEquals(UserRole.ADMIN, vm.uiState.value.loggedInUser?.role)
    assertEquals(CareLoopScreen.Admin(AdminTab.OVERVIEW), vm.uiState.value.currentScreen)
    assertNull(vm.uiState.value.accessDeniedMessage)
    assertEquals(AuditOutcome.ALLOWED, vm.uiState.value.auditLogs.first().outcome)

    // Logout
    vm.logout()
    assertFalse(vm.uiState.value.showSplash)
    assertEquals(CareLoopScreen.Login, vm.uiState.value.currentScreen)
    assertEquals("Logged out", vm.uiState.value.auditLogs.first().action)
  }

  @Test
  fun `send reminder enforces consent, fails on missing contact, delivers and supports Got it acknowledgement`() {
    val vm = CareLoopViewModel()
    val noConsentPatient = vm.uiState.value.patients.first { !it.smsConsent && !it.emailConsent }
    val missingContactPatient = vm.uiState.value.patients.first { !it.hasPhoneContact && !it.hasEmailContact }
    val consentPatient = vm.uiState.value.patients.first {
      it.hasConsentForPreferredMethod() && it.hasContactForPreferredMethod()
    }

    // 1. Blocked reminder -> no message delivered
    vm.sendReminder(noConsentPatient.id)
    val blocked = vm.uiState.value.reminderFeedbackByPatientId[noConsentPatient.id]
    assertEquals(ReminderFeedbackType.BLOCKED, blocked?.type)
    assertEquals("Blocked: no consent.", blocked?.message)
    assertTrue(vm.uiState.value.inboxMessages.none { it.patientId == noConsentPatient.id })
    assertEquals(AuditOutcome.DENIED, vm.uiState.value.auditLogs.first().outcome)

    // 2. Missing contact detail -> Failed status
    vm.sendReminder(missingContactPatient.id)
    val failed = vm.uiState.value.reminderFeedbackByPatientId[missingContactPatient.id]
    assertEquals(ReminderFeedbackType.FAILED, failed?.type)
    assertEquals(ReminderDeliveryStatus.FAILED, vm.uiState.value.reminderStatusByPatientId[missingContactPatient.id])
    assertTrue(vm.uiState.value.inboxMessages.none { it.patientId == missingContactPatient.id })

    // 3. Allowed reminder -> Delivered unread without medical note
    vm.sendReminder(consentPatient.id)
    val sent = vm.uiState.value.reminderFeedbackByPatientId[consentPatient.id]
    assertEquals(ReminderFeedbackType.SENT, sent?.type)
    assertEquals(ReminderDeliveryStatus.DELIVERED, vm.uiState.value.reminderStatusByPatientId[consentPatient.id])
    val delivered = vm.uiState.value.inboxMessages.filter { it.patientId == consentPatient.id }
    assertEquals(1, delivered.size)
    assertFalse(delivered.first().isRead)
    assertFalse(delivered.first().acknowledged)
    assertFalse(delivered.first().text.contains("blood", ignoreCase = true))

    // 4. Patient opens notification and taps "Got it" -> Acknowledged
    vm.signIn(consentPatient.demoEmail, DemoClinicData.DEMO_PASSWORD)
    vm.acknowledgeNotification(delivered.first().id)
    assertEquals(ReminderDeliveryStatus.ACKNOWLEDGED, vm.uiState.value.reminderStatusByPatientId[consentPatient.id])
    val ackMsg = vm.uiState.value.inboxMessages.first { it.id == delivered.first().id }
    assertTrue(ackMsg.isRead)
    assertTrue(ackMsg.acknowledged)
    assertTrue(vm.uiState.value.auditLogs.any { it.action.contains("Reminder acknowledged") })
  }

  @Test
  fun `clinic schedule allows Mon-Sat 9am-5pm and rejects Sundays and past dates`() {
    assertEquals(16, ClinicSchedule.ALL_SLOTS.size)
    assertEquals("9:00 AM", ClinicSchedule.slotLabel(0))
    assertEquals("4:30 PM", ClinicSchedule.slotLabel(15))

    val vm = CareLoopViewModel()
    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    val todayMillis = ClinicSchedule.todayUtcMidnightMillis()
    val yesterdayMillis = todayMillis - 86_400_000L
    assertFalse(ClinicSchedule.isSelectableClinicUtcDate(yesterdayMillis))

    cal.timeInMillis = todayMillis
    while (cal.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.SUNDAY) {
      cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
    }
    val sundayKey = ClinicSchedule.dateKeyFromUtcMillis(cal.timeInMillis)
    assertFalse(ClinicSchedule.isSelectableClinicUtcDate(cal.timeInMillis))
    assertTrue(vm.freeSlotsFor(DoctorId.ALI_KHAN, sundayKey, TimeOfDay.MORNING).isEmpty())

    cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
    val mondayKey = ClinicSchedule.dateKeyFromUtcMillis(cal.timeInMillis)
    assertTrue(ClinicSchedule.isSelectableClinicUtcDate(cal.timeInMillis))
    assertTrue(vm.freeSlotsFor(DoctorId.ALI_KHAN, mondayKey, TimeOfDay.MORNING).isNotEmpty())
  }

  @Test
  fun `appointment request, encrypted private note, booking, duplicate pending check, and double-booking guard work`() {
    val vm = CareLoopViewModel()
    val noConsentPatient = vm.uiState.value.patients.first { !it.smsConsent && !it.emailConsent }
    val activeDocAPatient = vm.uiState.value.patients.first { it.assignedDoctor == DoctorId.ALI_KHAN && !it.cancelled }
    val takenDateKey = activeDocAPatient.appointmentDateKey
    val takenSlot = activeDocAPatient.appointmentSlotIndex

    val freeOnTakenDate = vm.freeSlotsFor(DoctorId.ALI_KHAN, takenDateKey, null)
    assertTrue(freeOnTakenDate.none { it.index == takenSlot })

    vm.cancelAppointment(activeDocAPatient.id)
    val freeAfterCancel = vm.freeSlotsFor(DoctorId.ALI_KHAN, takenDateKey, null)
    assertTrue(freeAfterCancel.any { it.index == takenSlot })

    val targetDateKey = ClinicSchedule.openDateKeysWithin(1, 10).first()
    val freeSlot = vm.freeSlotsFor(DoctorId.IMRAN_QURESHI, targetDateKey, TimeOfDay.AFTERNOON).first()

    vm.submitAppointmentRequest(
      patientId = noConsentPatient.id,
      preferredDateKey = targetDateKey,
      preferredTimeOfDay = TimeOfDay.AFTERNOON,
      reason = "Skin rash evaluation",
      doctorId = DoctorId.IMRAN_QURESHI,
      preferredSlotIndex = freeSlot.index,
      privateNote = "Sensitive personal symptom note",
    )
    val req = vm.uiState.value.appointmentRequests.first()
    assertEquals(AppointmentRequestStatus.PENDING, req.status)
    assertEquals(DoctorId.IMRAN_QURESHI, req.doctorId)
    assertNotNull(req.encryptedPrivateNote)
    assertFalse(req.encryptedPrivateNote!!.contains("Sensitive"))
    assertTrue(noConsentPatient.id in vm.uiState.value.requestSentPatientIds)

    // Duplicate pending request blocked
    vm.submitAppointmentRequest(noConsentPatient.id, targetDateKey, TimeOfDay.MORNING, "Second request")
    assertEquals(1, vm.uiState.value.appointmentRequests.size)
    assertEquals("You already have a pending request.", vm.uiState.value.schedulingError)
    assertEquals(AuditOutcome.DENIED, vm.uiState.value.auditLogs.first().outcome)

    // Booking delivers in-app notification despite no SMS/Email consent
    vm.bookAppointmentRequestSlot(req.id, targetDateKey, freeSlot.index)
    assertEquals(AppointmentRequestStatus.BOOKED, vm.uiState.value.appointmentRequests.first().status)
    assertEquals(1, vm.uiState.value.bookedAppointments.count { it.patientId == noConsentPatient.id })
    assertEquals(
      1,
      vm.uiState.value.inboxMessages.count { it.recipientKey == DemoClinicData.patientAccountKey(noConsentPatient.id) },
    )

    // Double-booking attempt on the same doctor/date/slot is blocked
    val otherPatient = vm.uiState.value.patients.first { it.id != noConsentPatient.id }
    vm.submitAppointmentRequest(
      patientId = otherPatient.id,
      preferredDateKey = targetDateKey,
      preferredTimeOfDay = TimeOfDay.AFTERNOON,
      reason = "Checkup",
      doctorId = DoctorId.IMRAN_QURESHI,
    )
    val req2 = vm.uiState.value.appointmentRequests.first { it.patientId == otherPatient.id }
    vm.bookAppointmentRequestSlot(req2.id, targetDateKey, freeSlot.index)
    assertEquals("This slot is already booked.", vm.uiState.value.schedulingError)
    assertEquals(AppointmentRequestStatus.PENDING, vm.uiState.value.appointmentRequests.first { it.id == req2.id }.status)
    assertEquals(AuditOutcome.DENIED, vm.uiState.value.auditLogs.first().outcome)
  }

  @Test
  fun `offer another time and patient accept or decline update status and inbox`() {
    val vm = CareLoopViewModel()
    val patient = vm.uiState.value.patients.first { it.id == 6 }
    val dates = ClinicSchedule.openDateKeysWithin(1, 10)
    val reqDate = dates[0]
    val offerDate = dates[1]
    val offerSlot = vm.freeSlotsFor(patient.assignedDoctor, offerDate, TimeOfDay.MORNING).first()

    vm.submitAppointmentRequest(patient.id, reqDate, TimeOfDay.EVENING, "Lab review")
    val req = vm.uiState.value.appointmentRequests.first()

    vm.offerAnotherTime(req.id, offerDate, offerSlot.index)
    val offered = vm.uiState.value.appointmentRequests.first { it.id == req.id }
    assertEquals(AppointmentRequestStatus.NEW_TIME_OFFERED, offered.status)
    assertEquals(
      1,
      vm.uiState.value.inboxMessages.count { it.recipientKey == DemoClinicData.patientAccountKey(patient.id) },
    )

    vm.acceptOfferedTime(req.id)
    val accepted = vm.uiState.value.appointmentRequests.first { it.id == req.id }
    assertEquals(AppointmentRequestStatus.BOOKED, accepted.status)
    assertEquals(1, vm.uiState.value.bookedAppointments.count { it.patientId == patient.id })
    assertEquals(
      2,
      vm.uiState.value.inboxMessages.count { it.recipientKey == DemoClinicData.patientAccountKey(patient.id) },
    )

    val patient2 = vm.uiState.value.patients.first { it.id == 7 }
    vm.submitAppointmentRequest(patient2.id, reqDate, TimeOfDay.MORNING, "Consultation")
    val req2 = vm.uiState.value.appointmentRequests.first { it.patientId == patient2.id }
    vm.declineAppointmentRequest(req2.id, "Doctor not available")
    val declined = vm.uiState.value.appointmentRequests.first { it.id == req2.id }
    assertEquals(AppointmentRequestStatus.DECLINED, declined.status)
    assertEquals("Doctor not available", declined.declineReason)
    assertTrue(
      vm.uiState.value.inboxMessages.any {
        it.recipientKey == DemoClinicData.patientAccountKey(patient2.id) && it.text.contains("Doctor not available")
      }
    )
  }

  @Test
  fun `doctor approve on urgent patient chooses next free slot and hides medical notes in inbox`() {
    val vm = CareLoopViewModel()
    val urgentDoc = vm.uiState.value.patients.first { it.isUrgent && it.assignedDoctor == DoctorId.ALI_KHAN }

    vm.setDoctorDecision(DoctorId.ALI_KHAN, urgentDoc.id, DoctorDecision.APPROVED)
    assertEquals(
      DoctorDecision.APPROVED,
      vm.uiState.value.patients.first { it.id == urgentDoc.id }.doctorDecision,
    )
    val followUps = vm.uiState.value.followUpAppointments.filter { it.patientId == urgentDoc.id }
    assertEquals(1, followUps.size)
    val followUp = followUps.first()
    val collidingPatient = vm.uiState.value.patients.any {
      it.assignedDoctor == DoctorId.ALI_KHAN &&
        !it.cancelled &&
        it.appointmentDateKey == followUp.dateKey &&
        it.appointmentSlotIndex == followUp.slotIndex
    }
    assertFalse(collidingPatient)

    val msgs = vm.uiState.value.inboxMessages.filter { it.patientId == urgentDoc.id }
    assertEquals(1, msgs.size)
    assertFalse(msgs.first().text.contains("blood", ignoreCase = true))
  }

  @Test
  fun `doctor create follow-up adds recommendation and notifies patient`() {
    val vm = CareLoopViewModel()
    val patient = vm.uiState.value.patients.first { it.id == 6 }
    val initialCount = vm.uiState.value.doctorRecommendedFollowUps.size

    vm.createDoctorFollowUp(DoctorId.ALI_KHAN, patient.id, "Thyroid panel", 14)

    assertEquals(initialCount + 1, vm.uiState.value.doctorRecommendedFollowUps.size)
    val created = vm.uiState.value.doctorRecommendedFollowUps.first()
    assertEquals(patient.id, created.patientId)
    assertEquals("Thyroid panel", created.reason)
    assertEquals(14, created.dueInDays)
    assertTrue(vm.uiState.value.inboxMessages.any { it.patientId == patient.id && it.text.contains("Thyroid panel") })
    assertTrue(vm.uiState.value.auditLogs.any { it.action.contains("Follow-up created") })
  }

  @Test
  fun `patient reschedule updates appointment slot and prevents double-booking`() {
    val vm = CareLoopViewModel()
    val ayesha = vm.uiState.value.patients.first { it.id == 1 }
    val otherAliPatient = vm.uiState.value.patients.first { it.id != 1 && it.assignedDoctor == DoctorId.ALI_KHAN }

    // Attempt to reschedule onto another patient's taken slot -> denied
    vm.rescheduleAppointment(ayesha.id, otherAliPatient.appointmentDateKey, otherAliPatient.appointmentSlotIndex)
    assertEquals("This slot is already booked.", vm.uiState.value.schedulingError)
    assertEquals(AuditOutcome.DENIED, vm.uiState.value.auditLogs.first().outcome)

    // Reschedule onto free slot -> allowed, confirmed, rescheduled flag set
    val openDate = ClinicSchedule.openDateKeysWithin(1, 10).first()
    val freeSlot = vm.freeSlotsFor(DoctorId.ALI_KHAN, openDate).first()
    vm.rescheduleAppointment(ayesha.id, openDate, freeSlot.index)
    val updated = vm.uiState.value.patients.first { it.id == ayesha.id }
    assertTrue(updated.confirmed)
    assertTrue(updated.rescheduled)
    assertEquals(openDate, updated.appointmentDateKey)
    assertEquals(freeSlot.index, updated.appointmentSlotIndex)
    assertNull(vm.uiState.value.schedulingError)
  }

  @Test
  fun `reminder preferences auto-switch preferred method and save cleanly`() {
    val vm = CareLoopViewModel()
    val id = 1
    vm.updateSmsConsent(id, false)
    val afterSmsOff = vm.uiState.value.patients.first { it.id == id }
    assertFalse(afterSmsOff.smsConsent)
    assertEquals(ContactMethod.EMAIL, afterSmsOff.preferredContactMethod)
    assertTrue(id in vm.uiState.value.preferencesSavedPatientIds)

    vm.updateEmailConsent(id, false)
    val afterBothOff = vm.uiState.value.patients.first { it.id == id }
    assertEquals(ContactMethod.NONE, afterBothOff.preferredContactMethod)
    assertFalse(afterBothOff.hasConsentForPreferredMethod())

    vm.sendReminder(id)
    assertEquals("Blocked: no consent.", vm.uiState.value.reminderFeedbackByPatientId[id]?.message)

    vm.updateSmsConsent(id, true)
    assertEquals(ContactMethod.SMS, vm.uiState.value.patients.first { it.id == id }.preferredContactMethod)
  }

  @Test
  fun `doctor attended and no-show outcomes lock attendance and manage follow-up tasks`() {
    val vm = CareLoopViewModel()
    val patient = vm.uiState.value.patients.first { it.assignedDoctor == DoctorId.ALI_KHAN && !it.isUrgent }
    val initialMissed = patient.pastMissedAppointments

    vm.recordAttendance(DoctorId.ALI_KHAN, patient.id, AttendanceOutcome.ATTENDED)
    val updated = vm.uiState.value.patients.first { it.id == patient.id }
    assertEquals(AttendanceOutcome.ATTENDED, updated.attendanceOutcome)
    assertNotNull(updated.attendanceRecordedAt)
    assertEquals(initialMissed, updated.pastMissedAppointments)
    assertTrue(vm.uiState.value.missedFollowUpTasks.isEmpty())

    // Immutable once recorded
    vm.recordAttendance(DoctorId.ALI_KHAN, patient.id, AttendanceOutcome.NO_SHOW)
    assertEquals(AttendanceOutcome.ATTENDED, vm.uiState.value.patients.first { it.id == patient.id }.attendanceOutcome)

    val consentedPatient = vm.uiState.value.patients.first {
      it.assignedDoctor == DoctorId.ALI_KHAN && !it.isUrgent && it.id != patient.id && it.hasConsentForPreferredMethod()
    }
    vm.recordAttendance(DoctorId.ALI_KHAN, consentedPatient.id, AttendanceOutcome.NO_SHOW)
    val task1 = vm.uiState.value.missedFollowUpTasks.first { it.patientId == consentedPatient.id }
    vm.sendMissedFollowUpMessage(task1.id)
    val doneTask1 = vm.uiState.value.missedFollowUpTasks.first { it.id == task1.id }
    assertTrue(doneTask1.done)
    assertEquals(ReminderFeedbackType.SENT, doneTask1.feedback?.type)
    assertEquals(ReminderDeliveryStatus.DELIVERED, vm.uiState.value.reminderStatusByPatientId[consentedPatient.id])
  }

  @Test
  fun `aes-gcm decryption enforces strict role checks and logs every attempt`() {
    val vm = CareLoopViewModel()
    val docAPatient = vm.uiState.value.patients.first { it.assignedDoctor == DoctorId.ALI_KHAN && it.isUrgent }
    val docBPatient = vm.uiState.value.patients.first { it.assignedDoctor == DoctorId.SANA_RAUF && it.isUrgent }

    vm.signIn(DoctorId.ALI_KHAN.defaultEmail, DemoClinicData.DEMO_PASSWORD)
    assertEquals(
      "Abnormal blood test, needs recheck",
      vm.uiState.value.decryptedMedicalNotesByPatientId[docAPatient.id],
    )
    assertNull(vm.uiState.value.decryptedMedicalNotesByPatientId[docBPatient.id])
    assertTrue(vm.uiState.value.decryptedContactsByPatientId.isEmpty())

    assertEquals("Abnormal blood test, needs recheck", vm.attemptDecryptMedicalNote(docAPatient.id))
    assertEquals(AuditOutcome.ALLOWED, vm.uiState.value.auditLogs.first().outcome)

    assertNull(vm.attemptDecryptMedicalNote(docBPatient.id))
    assertEquals(AuditOutcome.DENIED, vm.uiState.value.auditLogs.first().outcome)

    assertNull(vm.attemptDecryptContact(docAPatient.id))
    assertEquals(AuditOutcome.DENIED, vm.uiState.value.auditLogs.first().outcome)

    vm.signIn(DemoClinicData.RECEPTIONIST_EMAIL, DemoClinicData.DEMO_PASSWORD)
    assertEquals(15, vm.uiState.value.decryptedContactsByPatientId.size)
    assertTrue(vm.uiState.value.decryptedMedicalNotesByPatientId.isEmpty())
    assertEquals("+92 300 4128901", vm.uiState.value.decryptedContactsByPatientId[docAPatient.id]?.phone)

    vm.signIn(docAPatient.demoEmail, DemoClinicData.DEMO_PASSWORD)
    assertEquals(1, vm.uiState.value.decryptedContactsByPatientId.size)
    assertNotNull(vm.uiState.value.decryptedContactsByPatientId[docAPatient.id])
    assertNull(vm.uiState.value.decryptedContactsByPatientId[docBPatient.id])
    assertTrue(vm.uiState.value.decryptedMedicalNotesByPatientId.isEmpty())

    vm.logout()
    assertTrue(vm.uiState.value.decryptedContactsByPatientId.isEmpty())
    assertTrue(vm.uiState.value.decryptedMedicalNotesByPatientId.isEmpty())
  }

  @Test
  fun `admin security tab is admin-only and never decrypts medical notes`() {
    val vm = CareLoopViewModel()

    vm.signIn(DemoClinicData.RECEPTIONIST_EMAIL, DemoClinicData.DEMO_PASSWORD)
    vm.openAdminSecurity()
    assertEquals(CareLoopScreen.Receptionist(), vm.uiState.value.currentScreen)
    assertEquals("Access denied", vm.uiState.value.accessDeniedMessage)
    assertEquals(AuditOutcome.DENIED, vm.uiState.value.auditLogs.first().outcome)

    vm.signIn(DemoClinicData.ADMIN_EMAIL, DemoClinicData.DEMO_PASSWORD)
    vm.openAdminSecurity()
    assertEquals(CareLoopScreen.Admin(AdminTab.SECURITY), vm.uiState.value.currentScreen)
    assertNull(vm.uiState.value.accessDeniedMessage)
    assertTrue(vm.uiState.value.decryptedMedicalNotesByPatientId.isEmpty())
    assertTrue(vm.uiState.value.decryptedContactsByPatientId.isEmpty())
  }

  @Test
  fun `audit log forms a sha256 hash chain and verify integrity detects tampering`() {
    val vm = CareLoopViewModel()
    vm.signIn(DemoClinicData.RECEPTIONIST_EMAIL, DemoClinicData.DEMO_PASSWORD)
    vm.sendReminder(1)
    vm.signIn(DemoClinicData.ADMIN_EMAIL, DemoClinicData.DEMO_PASSWORD)

    val logsChronological = vm.uiState.value.auditLogs.sortedBy { it.id }
    assertTrue(logsChronological.size >= 4)
    assertEquals(CareLoopCrypto.GENESIS_HASH, logsChronological.first().previousHash)
    for (i in 1 until logsChronological.size) {
      assertEquals(logsChronological[i - 1].hash, logsChronological[i].previousHash)
      assertEquals(64, logsChronological[i].hash.length)
    }

    vm.verifyAuditLogIntegrity()
    val validResult = vm.uiState.value.auditIntegrityResult
    assertNotNull(validResult)
    assertTrue(validResult!!.verified)
    assertEquals("Audit log verified, no changes detected", validResult.message)

    val targetEntry = logsChronological[1]
    vm.tamperAuditEntryForTest(targetEntry.id, "Tampered action")
    vm.verifyAuditLogIntegrity()
    val tamperedResult = vm.uiState.value.auditIntegrityResult
    assertNotNull(tamperedResult)
    assertFalse(tamperedResult!!.verified)
    assertTrue(tamperedResult.message.contains("Entry #${targetEntry.id}"))
  }

  @Test
  fun `profile updates persist name, email for sign-in, and photo data url`() {
    val vm = CareLoopViewModel()
    vm.signIn("ayesha.malik@careloop.demo", DemoClinicData.DEMO_PASSWORD)
    vm.updateUserProfile(
      name = "Ayesha Khan",
      phone = "+92 300 9998887",
      email = "ayesha.khan@careloop.demo",
      photoDataUrl = "data:image/jpeg;base64,AAAA",
    )
    assertTrue(vm.uiState.value.profileSaved)
    vm.logout()

    // Old email no longer signs in; new email signs in cleanly
    vm.signIn("ayesha.malik@careloop.demo", DemoClinicData.DEMO_PASSWORD)
    assertNotNull(vm.uiState.value.loginError)
    vm.signIn("ayesha.khan@careloop.demo", DemoClinicData.DEMO_PASSWORD)
    assertNull(vm.uiState.value.loginError)
    assertEquals("Ayesha Khan", vm.uiState.value.loggedInUser?.displayName)
    assertEquals(
      "data:image/jpeg;base64,AAAA",
      vm.uiState.value.userProfiles[DemoClinicData.patientAccountKey(1)]?.photoDataUrl,
    )
  }

  @Test
  fun `doctor ai assistant excludes medical notes, logs usage only, and handles unavailability`() {
    val capturedContexts = mutableListOf<DoctorAssistantContext>()
    val fakeGateway = object : CareLoopAiGateway {
      override suspend fun draftReminder(request: ReminderDraftRequest): AiTextResponse =
        AiTextResponse(text = "Reminder", usedFallback = false)

      override suspend fun summarizeDoctorTopRisk(
        doctorName: String,
        topPatients: List<HighRiskSummaryPatient>,
      ): AiTextResponse = AiTextResponse(text = "Summary", usedFallback = false)

      override suspend fun askDoctorAssistant(
        context: DoctorAssistantContext,
        question: String,
      ): AiTextResponse {
        capturedContexts += context
        return AiTextResponse(
          text = FirebaseCareLoopAiGateway.REASON_NO_RESPONSE,
          usedFallback = true,
          errorReason = FirebaseCareLoopAiGateway.REASON_NO_RESPONSE,
        )
      }
    }

    val vm = CareLoopViewModel(fakeGateway)
    vm.signIn(DoctorId.ALI_KHAN.defaultEmail, DemoClinicData.DEMO_PASSWORD)
    vm.askDoctorAiAssistant(DoctorId.ALI_KHAN, "Who is high-risk?")

    assertEquals(1, capturedContexts.size)
    val ctx = capturedContexts.first()
    assertTrue(ctx.patients.none { it.riskReason.contains("blood", ignoreCase = true) })
    val chat = vm.uiState.value.doctorChatMessagesByDoctorId[DoctorId.ALI_KHAN].orEmpty()
    assertEquals(2, chat.size)
    val reply = chat.last()
    assertFalse(reply.isError)
    assertEquals(FirebaseCareLoopAiGateway.REASON_NO_RESPONSE, reply.failureReason)
    assertEquals("Answered from clinic data.", reply.sourceLabel)
    assertTrue(reply.text.contains("High-risk", ignoreCase = true))
    assertFalse(reply.text.contains("blood", ignoreCase = true))
    // Audit logs ONLY chat usage, never chat content
    assertTrue(vm.uiState.value.auditLogs.any { it.action.contains("AI assistant chat used") })
    assertTrue(vm.uiState.value.auditLogs.none { it.action.contains("Who is high-risk") })
  }

  @Test
  fun `backup doctor assistant recognizes all 5 intents and greeting without leaking medical notes`() {
    val fakeGateway = object : CareLoopAiGateway {
      override suspend fun draftReminder(request: ReminderDraftRequest): AiTextResponse =
        AiTextResponse(text = "Reminder", usedFallback = false)

      override suspend fun summarizeDoctorTopRisk(
        doctorName: String,
        topPatients: List<HighRiskSummaryPatient>,
      ): AiTextResponse = AiTextResponse(text = "Summary", usedFallback = false)

      override suspend fun askDoctorAssistant(
        context: DoctorAssistantContext,
        question: String,
      ): AiTextResponse = AiTextResponse(
        text = FirebaseCareLoopAiGateway.REASON_NO_INTERNET,
        usedFallback = true,
        errorReason = FirebaseCareLoopAiGateway.REASON_NO_INTERNET,
      )
    }

    val vm = CareLoopViewModel(fakeGateway)
    vm.signIn(DoctorId.ALI_KHAN.defaultEmail, DemoClinicData.DEMO_PASSWORD)

    val queries = listOf(
      "hi",
      "today's appointments",
      "who is high-risk",
      "missed appointments",
      "urgent patients",
      "who is unconfirmed",
    )
    queries.forEach { q ->
      vm.askDoctorAiAssistant(DoctorId.ALI_KHAN, q)
    }

    val replies = vm.uiState.value.doctorChatMessagesByDoctorId[DoctorId.ALI_KHAN].orEmpty()
      .filter { !it.isUser }
    assertEquals(6, replies.size)
    replies.forEach { msg ->
      assertEquals(FirebaseCareLoopAiGateway.REASON_NO_INTERNET, msg.failureReason)
      assertEquals("Answered from clinic data.", msg.sourceLabel)
      assertFalse(msg.text.contains("BP ", ignoreCase = true))
      assertFalse(msg.text.contains("cholesterol", ignoreCase = true))
    }
    assertTrue(replies[0].text.contains("Hello", ignoreCase = true))
    assertTrue(replies[0].text.contains("urgent patients", ignoreCase = true))
    assertTrue(replies[1].text.contains("today", ignoreCase = true))
    assertTrue(replies[2].text.contains("High-risk", ignoreCase = true))
    assertTrue(replies[3].text.contains("missed", ignoreCase = true))
    assertTrue(replies[4].text.contains("Urgent", ignoreCase = true))
    assertTrue(replies[5].text.contains("Unconfirmed", ignoreCase = true))
  }

  @Test
  fun `role settings persist, control sound and popups, set Saved, and log audit entries`() {
    var savedSnapshot: PersistedCareLoopSnapshot? = null
    val testStorage = object : CareLoopStorage {
      override fun load(): PersistedCareLoopSnapshot? = savedSnapshot
      override fun save(snapshot: PersistedCareLoopSnapshot) {
        savedSnapshot = snapshot
      }
      override fun clear() {
        savedSnapshot = null
      }
      override fun registerExternalChangeListener(onChanged: () -> Unit): () -> Unit = {}
    }

    val vm = CareLoopViewModel(FirebaseCareLoopAiGateway(), testStorage)
    vm.signIn(DemoClinicData.RECEPTIONIST_EMAIL, DemoClinicData.DEMO_PASSWORD)
    vm.updateNotificationSound(false)
    assertFalse(CareLoopSoundPlayer.isSoundEnabled)
    vm.updatePopupAlerts(false)
    vm.updateReceptionistQueueSort(ReceptionistQueueSort.TIME)
    vm.updateReceptionistHighRiskFirst(true)
    assertTrue(vm.uiState.value.settingsSaved)

    // Pop-up alerts disabled -> sending reminder adds inbox message without topToastMessage
    val target = vm.uiState.value.patients.first { it.hasConsentForPreferredMethod() && it.hasContactForPreferredMethod() }
    vm.sendReminder(target.id)
    vm.signIn(target.demoEmail, DemoClinicData.DEMO_PASSWORD)
    // Confirm a new alert while popupAlertsEnabled is false should not show top toast
    vm.signIn(DemoClinicData.RECEPTIONIST_EMAIL, DemoClinicData.DEMO_PASSWORD)
    assertNull(vm.uiState.value.topToastMessage)

    vm.signIn(DoctorId.ALI_KHAN.defaultEmail, DemoClinicData.DEMO_PASSWORD)
    vm.updateDoctorDefaultFollowUpDays(14)
    vm.updateDoctorAiAssistantVisible(false)
    assertEquals(14, vm.uiState.value.appSettings.doctorDefaultFollowUpDays)
    assertFalse(vm.uiState.value.appSettings.doctorAiAssistantVisible)

    // Reopen ViewModel -> settings and sound flag survive without overwriting saved data
    val vm2 = CareLoopViewModel(FirebaseCareLoopAiGateway(), testStorage)
    assertFalse(vm2.uiState.value.appSettings.notificationSoundEnabled)
    assertFalse(vm2.uiState.value.appSettings.popupAlertsEnabled)
    assertEquals(ReceptionistQueueSort.TIME, vm2.uiState.value.appSettings.receptionistQueueSort)
    assertTrue(vm2.uiState.value.appSettings.receptionistHighRiskFirst)
    assertEquals(14, vm2.uiState.value.appSettings.doctorDefaultFollowUpDays)
    assertFalse(vm2.uiState.value.appSettings.doctorAiAssistantVisible)
    assertFalse(CareLoopSoundPlayer.isSoundEnabled)
    assertTrue(vm2.uiState.value.auditLogs.any { it.action.contains("Settings updated") })
  }

  @Test
  fun `sample notifications seeded on start, missed history matches count, and real events generate doctor and admin alerts`() {
    val vm = CareLoopViewModel()

    // Synthetic missed history: >= 5 patients have >= 2 missed appointments & exact match
    val patients = vm.uiState.value.patients
    assertTrue(patients.count { it.pastMissedAppointments >= 2 } >= 5)
    patients.forEach { p ->
      assertEquals(p.pastMissedAppointments, p.missedHistory.size)
      val keys = p.missedHistory.map { it.dateKey }
      assertEquals(keys.sortedDescending(), keys)
    }

    // Starting sample notifications: 3-4 for each doctor and Admin
    DoctorId.entries.forEach { doc ->
      vm.signIn(doc.defaultEmail, DemoClinicData.DEMO_PASSWORD)
      val docAlerts = vm.notificationsForCurrentUser()
      assertTrue("Expected 3..4 starting notifications for $doc, got ${docAlerts.size}", docAlerts.size in 3..4)
      assertTrue(docAlerts.all { it.patientId == 0 })
    }

    vm.signIn(DemoClinicData.ADMIN_EMAIL, DemoClinicData.DEMO_PASSWORD)
    val adminStartAlerts = vm.notificationsForCurrentUser()
    assertTrue(adminStartAlerts.size in 3..4)
    assertTrue(adminStartAlerts.none { it.text.contains("BP ", ignoreCase = true) })

    // No-show adds to missedHistory and generates MISSED_NO_SHOW doctor alert
    val aliToday = patients.first { it.assignedDoctor == DoctorId.ALI_KHAN && !it.isUrgent }
    val beforeMissedCount = aliToday.pastMissedAppointments
    vm.signIn(DoctorId.ALI_KHAN.defaultEmail, DemoClinicData.DEMO_PASSWORD)
    vm.recordAttendance(DoctorId.ALI_KHAN, aliToday.id, AttendanceOutcome.NO_SHOW)
    val updatedAliToday = vm.uiState.value.patients.first { it.id == aliToday.id }
    assertEquals(beforeMissedCount + 1, updatedAliToday.pastMissedAppointments)
    assertEquals(beforeMissedCount + 1, updatedAliToday.missedHistory.size)
    val noShowAlert = vm.notificationsForCurrentUser().first {
      it.category == NotificationCategory.MISSED_NO_SHOW && it.targetPatientId == aliToday.id
    }
    vm.openNotificationTarget(noShowAlert.id)
    assertEquals(CareLoopScreen.Doctor(DoctorId.ALI_KHAN, DoctorTab.PATIENTS), vm.uiState.value.currentScreen)
    assertEquals(aliToday.id, vm.uiState.value.focusedDoctorPatientId)

    // Patient turning off consent triggers Admin CONSENT_OFF alert; confirmation triggers Doctor CONFIRMED alert
    val patient2 = patients.first { it.id == 2 }
    vm.signIn(patient2.demoEmail, DemoClinicData.DEMO_PASSWORD)
    vm.updateSmsConsent(patient2.id, false)
    vm.confirmAppointment(patient2.id)

    vm.signIn(DemoClinicData.ADMIN_EMAIL, DemoClinicData.DEMO_PASSWORD)
    assertTrue(
      vm.notificationsForCurrentUser().any {
        it.category == NotificationCategory.CONSENT_OFF && it.targetPatientId == patient2.id
      },
    )
    vm.verifyAuditLogIntegrity()
    assertTrue(vm.notificationsForCurrentUser().any { it.category == NotificationCategory.AUDIT_INTEGRITY })
  }

  @Test
  fun `storage persistence and admin-only reset demo data work correctly`() {
    var savedSnapshot: PersistedCareLoopSnapshot? = null
    val testStorage = object : CareLoopStorage {
      override fun load(): PersistedCareLoopSnapshot? = savedSnapshot
      override fun save(snapshot: PersistedCareLoopSnapshot) {
        savedSnapshot = snapshot
      }
      override fun clear() {
        savedSnapshot = null
      }
      override fun registerExternalChangeListener(onChanged: () -> Unit): () -> Unit = {}
    }

    val vm1 = CareLoopViewModel(FirebaseCareLoopAiGateway(), testStorage)
    vm1.signIn(DemoClinicData.RECEPTIONIST_EMAIL, DemoClinicData.DEMO_PASSWORD)
    vm1.confirmAppointment(1)
    assertNotNull(savedSnapshot)

    // Reopen ViewModel with persisted storage -> confirmation state is preserved
    val vm2 = CareLoopViewModel(FirebaseCareLoopAiGateway(), testStorage)
    assertTrue(vm2.uiState.value.patients.first { it.id == 1 }.confirmed)

    // Non-admin cannot reset demo data
    vm2.signIn(DemoClinicData.RECEPTIONIST_EMAIL, DemoClinicData.DEMO_PASSWORD)
    vm2.resetDemoData()
    assertEquals("Access denied", vm2.uiState.value.accessDeniedMessage)
    assertTrue(vm2.uiState.value.patients.first { it.id == 1 }.confirmed)

    // Admin resets demo data cleanly
    vm2.signIn(DemoClinicData.ADMIN_EMAIL, DemoClinicData.DEMO_PASSWORD)
    vm2.resetDemoData()
    assertFalse(vm2.uiState.value.patients.first { it.id == 1 }.confirmed)
    assertTrue(vm2.notificationsForCurrentUser().size in 3..4)
  }

  @Test
  fun `ai reminder draft enforces consent first, sends minimal payload, supports edit approve discard, and logs audit`() {
    val recordedRequests = mutableListOf<ReminderDraftRequest>()
    val fakeGateway = object : CareLoopAiGateway {
      override suspend fun draftReminder(request: ReminderDraftRequest): AiTextResponse {
        recordedRequests += request
        return AiTextResponse(
          text = "Hi ${request.firstName}, friendly reminder for your visit with ${request.doctorName} (${request.appointmentDateTime}).",
          usedFallback = false,
        )
      }

      override suspend fun summarizeDoctorTopRisk(
        doctorName: String,
        topPatients: List<HighRiskSummaryPatient>,
      ): AiTextResponse = AiTextResponse(text = "Summary", usedFallback = false)

      override suspend fun askDoctorAssistant(
        context: DoctorAssistantContext,
        question: String,
      ): AiTextResponse = AiTextResponse(text = "Answer", usedFallback = false)
    }

    val vm = CareLoopViewModel(fakeGateway)
    vm.signIn(DemoClinicData.RECEPTIONIST_EMAIL, DemoClinicData.DEMO_PASSWORD)

    val noConsentPatient = vm.uiState.value.patients.first { !it.smsConsent && !it.emailConsent }
    vm.draftReminderWithAi(noConsentPatient.id)
    assertTrue(recordedRequests.isEmpty())
    assertNull(vm.uiState.value.aiReminderDraftsByPatientId[noConsentPatient.id])
    assertEquals("Blocked: no consent", vm.uiState.value.reminderFeedbackByPatientId[noConsentPatient.id]?.message)

    val urgentConsented = vm.uiState.value.patients.first { it.isUrgent && it.hasConsentForPreferredMethod() }
    vm.draftReminderWithAi(urgentConsented.id)
    assertEquals(1, recordedRequests.size)
    assertEquals(urgentConsented.firstName, recordedRequests.first().firstName)
    assertEquals(urgentConsented.assignedDoctor.displayName, recordedRequests.first().doctorName)

    vm.startEditingAiDraft(urgentConsented.id)
    vm.updateAiDraftText(urgentConsented.id, "Hi ${urgentConsented.firstName}, please confirm your clinic visit.")
    vm.finishEditingAiDraft(urgentConsented.id)
    vm.approveAndSendAiDraft(urgentConsented.id)
    assertNull(vm.uiState.value.aiReminderDraftsByPatientId[urgentConsented.id])
    assertEquals(ReminderDeliveryStatus.DELIVERED, vm.uiState.value.reminderStatusByPatientId[urgentConsented.id])
    assertEquals(1, vm.uiState.value.inboxMessages.count { it.patientId == urgentConsented.id })

    val secondConsented = vm.uiState.value.patients.first {
      it.id != urgentConsented.id && it.hasConsentForPreferredMethod() && it.hasContactForPreferredMethod()
    }
    vm.draftReminderWithAi(secondConsented.id)
    vm.discardAiDraft(secondConsented.id)
    assertNull(vm.uiState.value.aiReminderDraftsByPatientId[secondConsented.id])
    assertTrue(vm.uiState.value.inboxMessages.none { it.patientId == secondConsented.id })
  }
}
