package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.sound.SoundProfiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ChronoWake", appName)
  }

  @Test
  fun `verify sound profile catalog completeness`() {
    assertTrue(SoundProfiles.ALL.isNotEmpty())
    val theta = SoundProfiles.getById("binaural_theta")
    assertEquals(216f, theta.carrierFreqHz)
    assertEquals(6f, theta.beatFreqHz)
  }
}
