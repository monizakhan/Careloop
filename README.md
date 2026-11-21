# CareLoop

### Fewer missed appointments. Safer follow-ups.

A mobile healthcare app for small outpatient clinics that finds missed appointments **before** they happen, instead of after harm or wasted capacity has already occurred.

**Team:** Lavender++ | **Track:** HealthTech | **Challenge:** HS-0414, A Small Outpatient Clinic: Missed Appointments

> All patient data in this project is **synthetic**. No real patient information is used anywhere.

---

## Links

| | |
|---|---|
| Demo video | https://drive.google.com/file/d/1CwtdVmO3IWTRhwdfoH70IxOMhJtQo3tv/view?usp=drivesdk |
| Android app (APK) | https://drive.google.com/file/d/1sy-1-tpSogDD24wgn_wScqLdMRfE8yLl/view?usp=drivesdk |


---

## The Problem

In a small clinic, missed appointments and late follow-ups are usually discovered only after the visit was wasted or a patient has come to harm. Receptionists have no clear queue of who is likely to miss a visit, doctors find out about no-shows late, and patient preferences about how they want to be contacted are easy to overlook.

## Our Solution

CareLoop gives every person in the clinic a simple, focused workflow:

- **Receptionists** see a ranked queue of at-risk appointments, with a plain-language reason for each, and send reminders only where the patient has given consent.
- **Doctors** stay in control. They approve or override urgent follow-ups, record Attended or No-show, and recommend follow-ups. A recorded no-show instantly creates a follow-up task for the receptionist.
- **Patients** see their next appointment, book through a guided flow, manage their reminder preferences and consent, and acknowledge reminders.
- **Admins** see clinic-wide reports, the tamper-evident audit log, and security status.

**The loop:** Reminder → Appointment → Attended or No-show → Follow-up → Reminder again.

---

## Features

### Receptionist
- Risk-ranked queue with a one-line reason for every score
- Search by patient name or date, and filter by High / Medium / Low risk
- Send reminders with **consent enforcement** ("Blocked: no consent")
- Reminder status tracking: **Delivered, Failed, Acknowledged**
- Appointment requests with **Book, Offer another time, Decline**
- Follow-up tasks created automatically from doctor-recorded no-shows
- AI-drafted reminders (Gemini) that require human approval before sending

### Doctor
- Approve or override urgent follow-up decisions
- Mark appointments as **Attended** or **No-show**
- Create follow-up recommendations (7, 14 or 30 days)
- Missed-appointment history per patient
- Private medical notes hidden behind a "View note" link
- Floating AI assistant for schedule and risk questions, with a built-in backup that answers from clinic data

### Patient
- Personalised home screen with the next appointment highlighted
- Guided booking: specialization, doctor, date and time, reason, review
- Request status tracker (Request sent, Under review, Booked)
- Follow-ups due, and rescheduling after a missed appointment
- Settings for SMS and email consent and preferred contact method
- In-app notification bell with unread count

### Admin
- Clinic overview, reports and charts
- Hash-chained audit log with **Verify integrity**
- Security page showing encrypted data
- Doctor list, consent summary and demo data reset

---

## How CareLoop Meets the Challenge Requirements

| Requirement | How it is implemented |
|---|---|
| **Prioritised reminders and follow-up queues** | Explainable risk score ranks the receptionist queue and follow-up tasks |
| **Respecting consent and patient preferences** | Per-channel consent (SMS, email). Reminders are blocked without consent. Patients can change consent anytime and it takes effect immediately |
| **Strict patient-level access** | Role-based access. Patients see only their own data, doctors only their own patients, receptionists never see medical notes, and only the Admin can open the audit log |
| **Encryption** | Sensitive fields (medical notes, contact details) are encrypted with AES-GCM using the Web Crypto API and decrypted only after a role check passes |
| **Auditability** | Every action, allowed or denied, is logged. Each entry is linked to the previous one with a SHA-256 hash chain, and a Verify integrity button detects tampering |
| **Clinician oversight** | Doctors approve or override urgent decisions, and every AI-drafted message needs human approval |
| **Synthetic data** | 15 made-up patients. No real data |

### Explainable risk score

The score is simple on purpose, so staff can see **why** a patient is flagged:

| Factor | Points |
|---|---|
| Each past missed appointment | +30 |
| Appointment not confirmed | +20 |
| Clinically urgent | +25 |
| Appointment within 48 hours | +10 |

Every row also shows the reason in words, for example "Missed 2 before, not confirmed."

### Responsible use of AI

- Gemini drafts reminders and answers doctor questions, but **a human always approves** before anything is sent.
- Gemini never receives medical notes, only first names, times, counts and risk reasons.
- If Gemini is unavailable, the app falls back to a standard message or answers from clinic data.

---

## Try It Yourself

### 1. Install the app
1. Download the APK from the link above onto an Android phone.
2. Open the file and allow installation from this source if Android asks.
3. Open **CareLoop**.

### 2. Sign in
Choose a role, then an account from the dropdown. The password is the same for every demo account:

**Password: `demo1234`**

### 3. Suggested walkthrough (about 3 minutes)
1. Sign in as **Receptionist** and look at the risk-ranked queue.
2. Send a reminder to a patient **without consent**. It is **blocked**.
3. Send a reminder to a patient **with consent**. It is delivered.
4. Sign in as that **Patient**, open the bell, and tap "Got it." The reminder becomes **Acknowledged**.
5. Sign in as the **Doctor** and approve an urgent follow-up. Mark another patient as **No-show**.
6. Back as the **Receptionist**, find the new follow-up task created by the no-show.
7. Sign in as **Admin**, open the **Audit Log**, and tap **Verify integrity**. Then open **Security** to see encrypted data.

If the demo data ever looks wrong, go to **Admin, Settings, Reset demo data**.

---

## Security and Privacy Model

CareLoop is designed with healthcare-style safeguards:

- **Role-based access control** on every screen and action
- **Consent-first messaging**, enforced at the point of sending
- **AES-GCM encryption** for sensitive fields
- **Append-only, hash-chained audit log** with an integrity check
- **Data minimisation**: no medical details in reminders or AI prompts

### Honest limitations

This is a hackathon **prototype**. We want to be clear about what it is:

- Sign-in is simulated. Data and the encryption key are stored on the device, not on a secure server.
- Reminders are **simulated**. No real SMS or email is sent, and notifications work only inside the app.
- Access rules are enforced in the app itself. A production system must enforce them on the server.
- CareLoop is **not** certified for HIPAA or any other regulation. It is designed with HIPAA-style safeguards.

---

## Future Work

- Real authentication (Firebase Auth) and server-side access rules
- Key management through a managed key service instead of on-device keys
- Real SMS, email and push notifications
- Doctor leave and availability management
- Waiting list that offers cancelled slots to patients automatically
- Patient registration and role management for admins
- Urdu language support

---

## Built With

- Google AI Studio (Android app generation)
- Gemini API for reminder drafts and the doctor assistant
- Web Crypto API (AES-GCM and SHA-256)

---

## Team

**Lavender++**, HealthTech track
Moniza Khan

*Built for the Build for Campus hackathon, organised by Google Developer Groups.*
