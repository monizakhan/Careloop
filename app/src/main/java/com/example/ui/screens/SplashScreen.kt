package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.CareLoopLogoMark
import com.example.ui.theme.InterFamily
import com.example.ui.theme.SidebarDarkGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
  val logoAlpha = remember { Animatable(0f) }
  val logoScale = remember { Animatable(0.88f) }
  val titleAlpha = remember { Animatable(0f) }
  val taglineAlpha = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    launch {
      logoAlpha.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
      )
    }
    launch {
      logoScale.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
      )
    }
    launch {
      delay(450)
      titleAlpha.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
      )
    }
    launch {
      delay(900)
      taglineAlpha.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
      )
    }
    delay(2450)
    onFinished()
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(SidebarDarkGreen)
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .padding(24.dp)
      .testTag("splash_screen"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      CareLoopLogoMark(
        size = 84.dp,
        onDarkBackground = true,
        modifier = Modifier
          .alpha(logoAlpha.value)
          .scale(logoScale.value),
      )
      Spacer(modifier = Modifier.height(20.dp))
      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineMedium.copy(
          fontFamily = InterFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 30.sp,
        ),
        color = Color.White,
        modifier = Modifier.alpha(titleAlpha.value),
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = stringResource(R.string.login_tagline),
        style = MaterialTheme.typography.bodyLarge.copy(
          fontFamily = InterFamily,
          fontSize = 15.sp,
        ),
        color = Color.White.copy(alpha = 0.85f),
        textAlign = TextAlign.Center,
        modifier = Modifier.alpha(taglineAlpha.value),
      )
    }
  }
}
