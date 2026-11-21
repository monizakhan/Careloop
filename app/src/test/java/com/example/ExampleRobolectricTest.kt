package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AiTextResponse
import com.example.ai.CareLoopAiGateway
import com.example.ai.DoctorAssistantContext
import com.example.ai.HighRiskSummaryPatient
import com.example.ai.ReminderDraftRequest
import com.example.data.DemoClinicData
import com.example.data.SharedPrefsCareLoopStorage
import com.example.viewmodel.CareLoopViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CareLoop", appName)
  }

  @Test
  fun `shared preferences storage persists state and encryption key across ViewModel instances`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val fakeAi = object : CareLoopAiGateway {
      override suspend fun draftReminder(request: ReminderDraftRequest) = AiTextResponse("", true)
      override suspend fun summarizeDoctorTopRisk(doctorName: String, topPatients: List<HighRiskSummaryPatient>) = AiTextResponse("", true)
      override suspend fun askDoctorAssistant(context: DoctorAssistantContext, question: String) = AiTextResponse("AI unavailable.", true)
    }

    val storage1 = SharedPrefsCareLoopStorage(context)
    storage1.clear()
    val vm1 = CareLoopViewModel(fakeAi, storage1)
    vm1.signIn(DemoClinicData.RECEPTIONIST_EMAIL, DemoClinicData.DEMO_PASSWORD)
    vm1.confirmAppointment(1)
    vm1.sendReminder(1)

    // Reopen app with new storage & ViewModel instance
    val storage2 = SharedPrefsCareLoopStorage(context)
    val vm2 = CareLoopViewModel(fakeAi, storage2)
    assertTrue(vm2.uiState.value.patients.first { it.id == 1 }.confirmed)
    assertFalse(vm2.uiState.value.inboxMessages.isEmpty())
    vm2.signIn(DemoClinicData.RECEPTIONIST_EMAIL, DemoClinicData.DEMO_PASSWORD)
    assertEquals("+92 300 4128901", vm2.uiState.value.decryptedContactsByPatientId[1]?.phone)
  }
}
