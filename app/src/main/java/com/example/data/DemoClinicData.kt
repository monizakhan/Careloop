package com.example.data

import com.example.model.AvatarGender
import com.example.model.ClinicSchedule
import com.example.model.ContactMethod
import com.example.model.DemoAccountItem
import com.example.model.DoctorId
import com.example.model.DoctorRecommendedFollowUp
import com.example.model.DoctorTab
import com.example.model.InboxMessage
import com.example.model.MissedAppointmentRecord
import com.example.model.NotificationCategory
import com.example.model.Patient
import com.example.model.UserProfile
import com.example.security.CareLoopCrypto
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DemoClinicData {
  const val DEMO_PASSWORD: String = "demo1234"
  const val RECEPTIONIST_EMAIL: String = "receptionist@careloop.demo"
  const val ADMIN_EMAIL: String = "admin@careloop.demo"

  const val ACCOUNT_RECEPTIONIST: String = "staff_receptionist"
  const val ACCOUNT_ADMIN: String = "staff_admin"

  fun doctorAccountKey(doctorId: DoctorId): String = "doctor_${doctorId.name}"
  fun patientAccountKey(patientId: Int): String = "patient_$patientId"

  fun createMissedHistory(
    patientId: Int,
    count: Int,
    doctorName: String,
  ): List<MissedAppointmentRecord> {
    if (count <= 0) return emptyList()
    val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val labelFmt = SimpleDateFormat("MMM d, yyyy", Locale.US)
    val baseOffsetsDays = listOf(19, 47, 82, 118, 152)
    val records = mutableListOf<MissedAppointmentRecord>()
    for (idx in 0 until count) {
      val baseDays = baseOffsetsDays.getOrElse(idx) { 155 + (idx - baseOffsetsDays.size) * 14 }
      val daysAgo = (baseDays + (patientId % 5) * 3).coerceIn(3, 175)
      val cal = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -daysAgo)
        if (get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
          add(Calendar.DAY_OF_YEAR, -1)
        }
      }
      records += MissedAppointmentRecord(
        id = patientId * 100L + idx + 1L,
        dateKey = keyFmt.format(cal.time),
        dateLabel = labelFmt.format(cal.time),
        doctorName = doctorName,
      )
    }
    return records.sortedByDescending { it.dateKey }
  }

  fun createFakePatients(crypto: CareLoopCrypto = CareLoopCrypto()): List<Patient> {
    val openDates = ClinicSchedule.openDateKeysWithin(startOffsetDays = 0, daysWindow = 7)
    fun dateAt(dayOffset: Int): String = openDates[dayOffset.coerceIn(0, openDates.lastIndex)]

    fun makePatient(
      id: Int,
      name: String,
      age: Int,
      gender: AvatarGender,
      assignedDoctor: DoctorId,
      appointmentHoursFromNow: Int,
      dayOffset: Int,
      slotIndex: Int,
      pastMissedAppointments: Int,
      confirmed: Boolean,
      preferredContactMethod: ContactMethod,
      smsConsent: Boolean,
      emailConsent: Boolean,
      phone: String,
      medicalNote: String,
      isUrgent: Boolean = false,
      overrideEmail: String? = null,
    ): Patient {
      val dateKey = dateAt(dayOffset)
      val plainEmail = overrideEmail ?: (name.lowercase(Locale.US).trim().replace(" ", ".") + "@careloop.demo")
      return Patient(
        id = id,
        name = name,
        age = age,
        gender = gender,
        assignedDoctor = assignedDoctor,
        appointmentHoursFromNow = appointmentHoursFromNow,
        appointmentDateKey = dateKey,
        appointmentSlotIndex = slotIndex,
        appointmentTimeLabel = ClinicSchedule.formatDateAndSlot(dateKey, slotIndex),
        pastMissedAppointments = pastMissedAppointments,
        missedHistory = createMissedHistory(id, pastMissedAppointments, assignedDoctor.displayName),
        confirmed = confirmed,
        preferredContactMethod = preferredContactMethod,
        smsConsent = smsConsent,
        emailConsent = emailConsent,
        encryptedPhone = if (phone.isBlank()) "" else crypto.encrypt(phone),
        encryptedEmail = if (plainEmail.isBlank()) "" else crypto.encrypt(plainEmail),
        encryptedMedicalNote = crypto.encrypt(medicalNote),
        hasPhoneContact = phone.isNotBlank(),
        hasEmailContact = plainEmail.isNotBlank(),
        isUrgent = isUrgent,
        accountEmailOverride = if (plainEmail.isBlank()) name.lowercase(Locale.US).trim().replace(" ", ".") + "@careloop.demo" else null,
      )
    }

    return listOf(
      // Urgent #1 (Dr. Ali Khan, General Medicine) - high risk, has consent
      makePatient(
        id = 1,
        name = "Ayesha Malik",
        age = 54,
        gender = AvatarGender.FEMALE,
        assignedDoctor = DoctorId.ALI_KHAN,
        appointmentHoursFromNow = 16,
        dayOffset = 0,
        slotIndex = 2, // 10:00 AM
        pastMissedAppointments = 2,
        confirmed = false,
        preferredContactMethod = ContactMethod.SMS,
        smsConsent = true,
        emailConsent = true,
        phone = "+92 300 4128901",
        medicalNote = "Abnormal blood test, needs recheck",
        isUrgent = true,
      ),
      // Urgent #2 (Dr. Sana Rauf, Cardiology) - high risk, has consent
      makePatient(
        id = 2,
        name = "Hamza Raza",
        age = 62,
        gender = AvatarGender.MALE,
        assignedDoctor = DoctorId.SANA_RAUF,
        appointmentHoursFromNow = 26,
        dayOffset = 1,
        slotIndex = 4, // 11:00 AM
        pastMissedAppointments = 2,
        confirmed = false,
        preferredContactMethod = ContactMethod.EMAIL,
        smsConsent = true,
        emailConsent = true,
        phone = "+92 301 5192044",
        medicalNote = "Elevated HbA1c and blood pressure reading, needs urgent recheck",
        isUrgent = true,
      ),
      // No consent #1 (Dr. Ali Khan)
      makePatient(
        id = 3,
        name = "Bilal Ahmed",
        age = 41,
        gender = AvatarGender.MALE,
        assignedDoctor = DoctorId.ALI_KHAN,
        appointmentHoursFromNow = 20,
        dayOffset = 0,
        slotIndex = 6, // 12:00 PM
        pastMissedAppointments = 2,
        confirmed = false,
        preferredContactMethod = ContactMethod.SMS,
        smsConsent = false,
        emailConsent = false,
        phone = "+92 321 7843102",
        medicalNote = "Cholesterol panel follow-up",
      ),
      // No consent #2 (Dr. Sana Rauf)
      makePatient(
        id = 4,
        name = "Fatima Noor",
        age = 29,
        gender = AvatarGender.FEMALE,
        assignedDoctor = DoctorId.SANA_RAUF,
        appointmentHoursFromNow = 38,
        dayOffset = 1,
        slotIndex = 8, // 1:00 PM
        pastMissedAppointments = 1,
        confirmed = false,
        preferredContactMethod = ContactMethod.EMAIL,
        smsConsent = false,
        emailConsent = false,
        phone = "+92 333 9041155",
        medicalNote = "Seasonal asthma review",
      ),
      // No consent #3 (Dr. Ali Khan)
      makePatient(
        id = 5,
        name = "Kamran Sheikh",
        age = 71,
        gender = AvatarGender.MALE,
        assignedDoctor = DoctorId.ALI_KHAN,
        appointmentHoursFromNow = 74,
        dayOffset = 2,
        slotIndex = 1, // 9:30 AM
        pastMissedAppointments = 0,
        confirmed = false,
        preferredContactMethod = ContactMethod.NONE,
        smsConsent = false,
        emailConsent = false,
        phone = "+92 302 6428819",
        medicalNote = "Routine blood pressure check",
      ),
      makePatient(
        id = 6,
        name = "Sana Iqbal",
        age = 34,
        gender = AvatarGender.FEMALE,
        assignedDoctor = DoctorId.ALI_KHAN,
        appointmentHoursFromNow = 12,
        dayOffset = 0,
        slotIndex = 10, // 2:00 PM
        pastMissedAppointments = 2,
        confirmed = true,
        preferredContactMethod = ContactMethod.SMS,
        smsConsent = true,
        emailConsent = true,
        phone = "+92 345 2109384",
        medicalNote = "Thyroid medication adjustment",
      ),
      makePatient(
        id = 7,
        name = "Usman Tariq",
        age = 47,
        gender = AvatarGender.MALE,
        assignedDoctor = DoctorId.SANA_RAUF,
        appointmentHoursFromNow = 42,
        dayOffset = 1,
        slotIndex = 12, // 3:00 PM
        pastMissedAppointments = 2,
        confirmed = false,
        preferredContactMethod = ContactMethod.SMS,
        smsConsent = true,
        emailConsent = false,
        phone = "+92 304 8317720",
        medicalNote = "Sleep quality and fatigue consult",
      ),
      makePatient(
        id = 8,
        name = "Hira Siddiqui",
        age = 26,
        gender = AvatarGender.FEMALE,
        assignedDoctor = DoctorId.ALI_KHAN,
        appointmentHoursFromNow = 28,
        dayOffset = 1,
        slotIndex = 7, // 12:30 PM
        pastMissedAppointments = 2,
        confirmed = false,
        preferredContactMethod = ContactMethod.EMAIL,
        smsConsent = false,
        emailConsent = true,
        phone = "+92 331 4450912",
        medicalNote = "Annual wellness physical",
      ),
      makePatient(
        id = 9,
        name = "Zain Abbas",
        age = 58,
        gender = AvatarGender.MALE,
        assignedDoctor = DoctorId.SANA_RAUF,
        appointmentHoursFromNow = 18,
        dayOffset = 0,
        slotIndex = 3, // 10:30 AM
        pastMissedAppointments = 1,
        confirmed = true,
        preferredContactMethod = ContactMethod.SMS,
        smsConsent = true,
        emailConsent = true,
        phone = "+92 306 5512980",
        medicalNote = "Joint stiffness follow-up",
      ),
      makePatient(
        id = 10,
        name = "Mariam Chaudhry",
        age = 37,
        gender = AvatarGender.FEMALE,
        assignedDoctor = DoctorId.ALI_KHAN,
        appointmentHoursFromNow = 96,
        dayOffset = 3,
        slotIndex = 9, // 1:30 PM
        pastMissedAppointments = 1,
        confirmed = true,
        preferredContactMethod = ContactMethod.EMAIL,
        smsConsent = true,
        emailConsent = true,
        phone = "+92 312 8894011",
        medicalNote = "Migraine prevention check-in",
      ),
      // Missing contact detail patient (has consent, but no phone & no email -> Failed reminder)
      makePatient(
        id = 11,
        name = "Ali Hassan",
        age = 45,
        gender = AvatarGender.MALE,
        assignedDoctor = DoctorId.SANA_RAUF,
        appointmentHoursFromNow = 64,
        dayOffset = 2,
        slotIndex = 11, // 2:30 PM
        pastMissedAppointments = 0,
        confirmed = false,
        preferredContactMethod = ContactMethod.SMS,
        smsConsent = true,
        emailConsent = true,
        phone = "",
        medicalNote = "Vitamin D supplementation review",
        overrideEmail = "",
      ),
      makePatient(
        id = 12,
        name = "Rabia Anwar",
        age = 66,
        gender = AvatarGender.FEMALE,
        assignedDoctor = DoctorId.ALI_KHAN,
        appointmentHoursFromNow = 30,
        dayOffset = 1,
        slotIndex = 0, // 9:00 AM
        pastMissedAppointments = 0,
        confirmed = true,
        preferredContactMethod = ContactMethod.EMAIL,
        smsConsent = false,
        emailConsent = true,
        phone = "+92 334 7120635",
        medicalNote = "Routine vaccination and checkup",
      ),
      makePatient(
        id = 13,
        name = "Saad Mehmood",
        age = 31,
        gender = AvatarGender.MALE,
        assignedDoctor = DoctorId.IMRAN_QURESHI,
        appointmentHoursFromNow = 14,
        dayOffset = 0,
        slotIndex = 5, // 11:30 AM
        pastMissedAppointments = 0,
        confirmed = true,
        preferredContactMethod = ContactMethod.SMS,
        smsConsent = true,
        emailConsent = true,
        phone = "+92 322 9548103",
        medicalNote = "Skin rash follow-up clearance",
      ),
      makePatient(
        id = 14,
        name = "Iqra Javed",
        age = 50,
        gender = AvatarGender.FEMALE,
        assignedDoctor = DoctorId.HINA_FAROOQ,
        appointmentHoursFromNow = 19,
        dayOffset = 0,
        slotIndex = 14, // 4:00 PM
        pastMissedAppointments = 0,
        confirmed = true,
        preferredContactMethod = ContactMethod.EMAIL,
        smsConsent = true,
        emailConsent = true,
        phone = "+92 315 6034782",
        medicalNote = "Pediatric guardian care consultation",
      ),
      makePatient(
        id = 15,
        name = "Noor Fatima",
        age = 60,
        gender = AvatarGender.FEMALE,
        assignedDoctor = DoctorId.HINA_FAROOQ,
        appointmentHoursFromNow = 32,
        dayOffset = 1,
        slotIndex = 15, // 4:30 PM
        pastMissedAppointments = 1,
        confirmed = false,
        preferredContactMethod = ContactMethod.SMS,
        smsConsent = true,
        emailConsent = false,
        phone = "+92 309 1784326",
        medicalNote = "Follow-up immunization consult",
      ),
    )
  }

  fun createInitialProfiles(
    patients: List<Patient>,
    crypto: CareLoopCrypto,
  ): Map<String, UserProfile> {
    val map = linkedMapOf<String, UserProfile>()
    map[ACCOUNT_RECEPTIONIST] = UserProfile(
      accountKey = ACCOUNT_RECEPTIONIST,
      name = "Clinic Desk",
      phone = "+92 300 5550101",
      email = RECEPTIONIST_EMAIL,
      gender = AvatarGender.FEMALE,
    )
    DoctorId.entries.forEach { doc ->
      val key = doctorAccountKey(doc)
      map[key] = UserProfile(
        accountKey = key,
        name = doc.displayName,
        phone = doc.defaultPhone,
        email = doc.defaultEmail,
        gender = doc.gender,
        doctorId = doc,
      )
    }
    map[ACCOUNT_ADMIN] = UserProfile(
      accountKey = ACCOUNT_ADMIN,
      name = "Clinic Admin",
      phone = "+92 300 5550199",
      email = ADMIN_EMAIL,
      gender = AvatarGender.MALE,
    )
    patients.forEach { p ->
      val key = patientAccountKey(p.id)
      val phone = if (p.hasPhoneContact) crypto.decrypt(p.encryptedPhone) else ""
      val email = if (p.hasEmailContact) crypto.decrypt(p.encryptedEmail) else p.demoEmail
      map[key] = UserProfile(
        accountKey = key,
        name = p.name,
        phone = phone,
        email = email,
        gender = p.gender,
        patientId = p.id,
      )
    }
    return map
  }

  fun createInitialDoctorFollowUps(): List<DoctorRecommendedFollowUp> {
    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }
    val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    val dateLabel = SimpleDateFormat("d MMM", Locale.US).format(cal.time)
    return listOf(
      DoctorRecommendedFollowUp(
        id = 1L,
        patientId = 1,
        doctorId = DoctorId.ALI_KHAN,
        reason = "Cholesterol recheck",
        dueInDays = 7,
        dueDateKey = dateKey,
        dueDateLabel = dateLabel,
        booked = false,
      ),
    )
  }

  fun createInitialStaffNotifications(
    patients: List<Patient>,
    startId: Long = 1L,
  ): Pair<List<InboxMessage>, Long> {
    var nextId = startId
    val now = System.currentTimeMillis()
    val fmt = SimpleDateFormat("MMM d, h:mm a", Locale.US)
    fun timeAgo(hoursAgo: Int): Pair<String, Long> {
      val cal = Calendar.getInstance().apply {
        timeInMillis = now - hoursAgo * 3_600_000L
      }
      return fmt.format(cal.time) to cal.timeInMillis
    }

    val messages = mutableListOf<InboxMessage>()

    fun addDocMsg(
      doctorId: DoctorId,
      targetPatientId: Int?,
      title: String,
      text: String,
      hoursAgo: Int,
      category: NotificationCategory,
      targetTab: DoctorTab,
    ) {
      val (label, epoch) = timeAgo(hoursAgo)
      messages += InboxMessage(
        id = nextId++,
        patientId = 0,
        recipientKey = doctorAccountKey(doctorId),
        title = title,
        channelLabel = "Doctor Alert",
        timestampLabel = label,
        text = text,
        isRead = false,
        isReminder = false,
        acknowledged = false,
        createdAtEpochMs = epoch,
        category = category,
        targetDoctorTab = targetTab,
        targetPatientId = targetPatientId,
      )
    }

    fun addAdminMsg(
      title: String,
      text: String,
      hoursAgo: Int,
      category: NotificationCategory,
    ) {
      val (label, epoch) = timeAgo(hoursAgo)
      messages += InboxMessage(
        id = nextId++,
        patientId = 0,
        recipientKey = "admin",
        title = title,
        channelLabel = "System Event",
        timestampLabel = label,
        text = text,
        isRead = false,
        isReminder = false,
        acknowledged = false,
        createdAtEpochMs = epoch,
        category = category,
      )
    }

    // Dr. Ali Khan (4 starting notifications from synthetic patients)
    val aliUrgent = patients.firstOrNull { it.assignedDoctor == DoctorId.ALI_KHAN && it.isUrgent }
    val aliConfirmed = patients.firstOrNull { it.assignedDoctor == DoctorId.ALI_KHAN && it.confirmed }
    val aliTomorrowHigh = patients.firstOrNull {
      it.assignedDoctor == DoctorId.ALI_KHAN && !it.confirmed && it.riskScore() >= 60 && !it.isUrgent
    }
    val aliMissed = patients.firstOrNull {
      it.assignedDoctor == DoctorId.ALI_KHAN && it.pastMissedAppointments > 0 && it.id != aliUrgent?.id
    }
    aliUrgent?.let {
      addDocMsg(
        DoctorId.ALI_KHAN,
        it.id,
        "Urgent decision needed",
        "${it.firstName} has an urgent follow-up waiting for your decision.",
        2,
        NotificationCategory.URGENT_DECISION,
        DoctorTab.NEEDS_DECISION,
      )
    }
    aliConfirmed?.let {
      addDocMsg(
        DoctorId.ALI_KHAN,
        it.id,
        "Appointment confirmed",
        "${it.firstName} confirmed their ${it.appointmentTimeLabel} appointment.",
        6,
        NotificationCategory.CONFIRMED,
        DoctorTab.TODAY,
      )
    }
    aliTomorrowHigh?.let {
      addDocMsg(
        DoctorId.ALI_KHAN,
        it.id,
        "Unconfirmed tomorrow",
        "${it.firstName} is high-risk and tomorrow's ${ClinicSchedule.slotLabel(it.appointmentSlotIndex)} visit is unconfirmed.",
        18,
        NotificationCategory.TOMORROW_UNCONFIRMED,
        DoctorTab.TODAY,
      )
    }
    aliMissed?.let {
      addDocMsg(
        DoctorId.ALI_KHAN,
        it.id,
        "Missed history alert",
        "${it.firstName} has ${it.pastMissedAppointments} past missed appointments on record.",
        28,
        NotificationCategory.MISSED_NO_SHOW,
        DoctorTab.PATIENTS,
      )
    }

    // Dr. Sana Rauf (4 starting notifications from synthetic patients)
    val sanaUrgent = patients.firstOrNull { it.assignedDoctor == DoctorId.SANA_RAUF && it.isUrgent }
    val sanaTomorrowHigh = patients.firstOrNull {
      it.assignedDoctor == DoctorId.SANA_RAUF && !it.confirmed && it.riskScore() >= 60 && !it.isUrgent
    }
    val sanaConfirmed = patients.firstOrNull { it.assignedDoctor == DoctorId.SANA_RAUF && it.confirmed }
    val sanaMissed = patients.firstOrNull {
      it.assignedDoctor == DoctorId.SANA_RAUF && it.pastMissedAppointments > 0 && it.id != sanaUrgent?.id
    }
    sanaUrgent?.let {
      addDocMsg(
        DoctorId.SANA_RAUF,
        it.id,
        "Urgent decision needed",
        "${it.firstName} has an urgent follow-up waiting for your decision.",
        3,
        NotificationCategory.URGENT_DECISION,
        DoctorTab.NEEDS_DECISION,
      )
    }
    sanaTomorrowHigh?.let {
      addDocMsg(
        DoctorId.SANA_RAUF,
        it.id,
        "Unconfirmed tomorrow",
        "${it.firstName} is high-risk and tomorrow's ${ClinicSchedule.slotLabel(it.appointmentSlotIndex)} visit is unconfirmed.",
        8,
        NotificationCategory.TOMORROW_UNCONFIRMED,
        DoctorTab.TODAY,
      )
    }
    sanaConfirmed?.let {
      addDocMsg(
        DoctorId.SANA_RAUF,
        it.id,
        "Appointment confirmed",
        "${it.firstName} confirmed their ${it.appointmentTimeLabel} appointment.",
        22,
        NotificationCategory.CONFIRMED,
        DoctorTab.TODAY,
      )
    }
    sanaMissed?.let {
      addDocMsg(
        DoctorId.SANA_RAUF,
        it.id,
        "Missed history alert",
        "${it.firstName} has ${it.pastMissedAppointments} past missed appointment on record.",
        32,
        NotificationCategory.MISSED_NO_SHOW,
        DoctorTab.PATIENTS,
      )
    }

    // Dr. Imran Qureshi (3 starting notifications from synthetic patient)
    val imranPatient = patients.firstOrNull { it.assignedDoctor == DoctorId.IMRAN_QURESHI }
    if (imranPatient != null) {
      addDocMsg(
        DoctorId.IMRAN_QURESHI,
        imranPatient.id,
        "Appointment booked",
        "New appointment booked with ${imranPatient.firstName} for ${imranPatient.appointmentTimeLabel}.",
        4,
        NotificationCategory.NEW_BOOKING,
        DoctorTab.TODAY,
      )
      addDocMsg(
        DoctorId.IMRAN_QURESHI,
        imranPatient.id,
        "Appointment confirmed",
        "${imranPatient.firstName} confirmed their ${imranPatient.appointmentTimeLabel} visit.",
        14,
        NotificationCategory.CONFIRMED,
        DoctorTab.TODAY,
      )
      addDocMsg(
        DoctorId.IMRAN_QURESHI,
        imranPatient.id,
        "New time accepted",
        "${imranPatient.firstName} accepted the offered ${ClinicSchedule.slotLabel(imranPatient.appointmentSlotIndex)} slot.",
        27,
        NotificationCategory.OFFER_RESPONSE,
        DoctorTab.TODAY,
      )
    }

    // Dr. Hina Farooq (4 starting notifications from synthetic patients)
    val hinaPatients = patients.filter { it.assignedDoctor == DoctorId.HINA_FAROOQ }
    val hinaFirst = hinaPatients.firstOrNull()
    val hinaSecond = hinaPatients.getOrNull(1) ?: hinaFirst
    hinaFirst?.let {
      addDocMsg(
        DoctorId.HINA_FAROOQ,
        it.id,
        "Appointment booked",
        "New appointment booked with ${it.firstName} for ${it.appointmentTimeLabel}.",
        5,
        NotificationCategory.NEW_BOOKING,
        DoctorTab.TODAY,
      )
      addDocMsg(
        DoctorId.HINA_FAROOQ,
        it.id,
        "Appointment confirmed",
        "${it.firstName} confirmed their ${it.appointmentTimeLabel} appointment.",
        16,
        NotificationCategory.CONFIRMED,
        DoctorTab.TODAY,
      )
    }
    hinaSecond?.let {
      addDocMsg(
        DoctorId.HINA_FAROOQ,
        it.id,
        "Unconfirmed tomorrow",
        "${it.firstName} has an unconfirmed visit scheduled for ${it.appointmentTimeLabel}.",
        24,
        NotificationCategory.TOMORROW_UNCONFIRMED,
        DoctorTab.TODAY,
      )
      addDocMsg(
        DoctorId.HINA_FAROOQ,
        it.id,
        "Missed history alert",
        "${it.firstName} has ${it.pastMissedAppointments} past missed appointment on record.",
        36,
        NotificationCategory.MISSED_NO_SHOW,
        DoctorTab.PATIENTS,
      )
    }

    // Admin (4 starting system-wide notifications, zero medical info)
    val noConsentPatient = patients.firstOrNull { !it.smsConsent && !it.emailConsent }
    val missingContactPatient = patients.firstOrNull { !it.hasPhoneContact && !it.hasEmailContact }
    val consentOffPatient = patients.lastOrNull { !it.smsConsent && !it.emailConsent }
    noConsentPatient?.let {
      addAdminMsg(
        "Reminder blocked",
        "Reminder for ${it.name} was blocked because reminder consent is off.",
        2,
        NotificationCategory.CONSENT_BLOCKED,
      )
    }
    missingContactPatient?.let {
      addAdminMsg(
        "Delivery failed",
        "Reminder for ${it.name} failed to deliver due to missing contact details.",
        11,
        NotificationCategory.DELIVERY_FAILED,
      )
    }
    consentOffPatient?.let {
      addAdminMsg(
        "Consent turned off",
        "${it.name} has reminder consent turned off.",
        21,
        NotificationCategory.CONSENT_OFF,
      )
    }
    addAdminMsg(
      "Audit integrity verified",
      "Audit integrity check passed on clinic log chain.",
      34,
      NotificationCategory.AUDIT_INTEGRITY,
    )

    return messages.sortedByDescending { it.createdAtEpochMs } to nextId
  }

  fun buildDemoAccounts(
    patients: List<Patient>,
    profiles: Map<String, UserProfile> = emptyMap(),
  ): List<DemoAccountItem> {
    val receptionistProfile = profiles[ACCOUNT_RECEPTIONIST]
    val adminProfile = profiles[ACCOUNT_ADMIN]
    val staff = mutableListOf(
      DemoAccountItem(
        accountKey = ACCOUNT_RECEPTIONIST,
        email = receptionistProfile?.email ?: RECEPTIONIST_EMAIL,
        roleSummary = receptionistProfile?.name ?: "Clinic Desk",
      ),
    )
    DoctorId.entries.forEach { doc ->
      val key = doctorAccountKey(doc)
      val profile = profiles[key]
      val name = profile?.name ?: doc.displayName
      staff += DemoAccountItem(
        accountKey = key,
        email = profile?.email ?: doc.defaultEmail,
        roleSummary = "$name (${doc.specializationLabel})",
      )
    }
    staff += DemoAccountItem(
      accountKey = ACCOUNT_ADMIN,
      email = adminProfile?.email ?: ADMIN_EMAIL,
      roleSummary = adminProfile?.name ?: "Admin",
    )
    val patientAccounts = patients.map { patient ->
      val profile = profiles[patientAccountKey(patient.id)]
      val email = profile?.email?.takeIf { it.isNotBlank() } ?: patient.demoEmail
      DemoAccountItem(
        accountKey = patientAccountKey(patient.id),
        email = email,
        roleSummary = "${patient.name} (Patient)",
      )
    }
    return staff + patientAccounts
  }
}
