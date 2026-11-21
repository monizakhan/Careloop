package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AvatarGender
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.SidebarDarkGreen
import com.example.ui.theme.SoftGreenSurface
import kotlin.math.abs

private val SkinToneLight = Color(0xFFEBD0BA)
private val SkinToneWarm = Color(0xFFDAB193)
private val HairDark = Color(0xFF28332E)
private val HairSoftBrown = Color(0xFF4A4036)
private val CoatGreen = Color(0xFF1E634B)
private val ShirtNeutral = Color(0xFFE7ECE9)

@Composable
fun CareLoopAvatar(
  name: String,
  gender: AvatarGender,
  photoDataUrl: String? = null,
  size: Dp = 40.dp,
  modifier: Modifier = Modifier,
) {
  val decodedBitmap: ImageBitmap? = remember(photoDataUrl) {
    decodeDataUrlToImageBitmap(photoDataUrl)
  }

  Box(
    modifier = modifier
      .size(size)
      .clip(CircleShape)
      .background(SoftGreenSurface)
      .border(1.dp, HairlineBorder, CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    if (decodedBitmap != null) {
      Image(
        bitmap = decodedBitmap,
        contentDescription = "$name avatar",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      val variant = abs(name.hashCode()) % 2
      val skin = if (variant == 0) SkinToneLight else SkinToneWarm
      val hair = if (variant == 0) HairDark else HairSoftBrown
      val bgTint = if (variant == 0) Color(0xFFDDEBE3) else Color(0xFFE7EFEA)

      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = this.size.width
        val h = this.size.height

        drawCircle(color = bgTint)

        // Shoulders / collar
        val shoulderPath = Path().apply {
          moveTo(w * 0.14f, h * 1.02f)
          quadraticTo(w * 0.22f, h * 0.68f, w * 0.50f, h * 0.68f)
          quadraticTo(w * 0.78f, h * 0.68f, w * 0.86f, h * 1.02f)
          close()
        }
        drawPath(
          path = shoulderPath,
          color = if (variant == 0) SidebarDarkGreen else CoatGreen,
        )

        // Subtle inner collar
        val collarPath = Path().apply {
          moveTo(w * 0.39f, h * 0.70f)
          lineTo(w * 0.50f, h * 0.82f)
          lineTo(w * 0.61f, h * 0.70f)
          close()
        }
        drawPath(path = collarPath, color = ShirtNeutral)

        // Neck
        drawRoundRect(
          color = skin,
          topLeft = Offset(w * 0.43f, h * 0.56f),
          size = Size(w * 0.14f, h * 0.15f),
          cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
        )

        if (gender == AvatarGender.FEMALE) {
          // Back hair / headscarf drape in soft neutral & green tones
          val backHair = Path().apply {
            moveTo(w * 0.27f, h * 0.44f)
            quadraticTo(w * 0.27f, h * 0.18f, w * 0.50f, h * 0.18f)
            quadraticTo(w * 0.73f, h * 0.18f, w * 0.73f, h * 0.44f)
            lineTo(w * 0.76f, h * 0.67f)
            quadraticTo(w * 0.50f, h * 0.72f, w * 0.24f, h * 0.67f)
            close()
          }
          drawPath(path = backHair, color = hair)

          // Face
          drawOval(
            color = skin,
            topLeft = Offset(w * 0.33f, h * 0.27f),
            size = Size(w * 0.34f, h * 0.36f),
          )

          // Front bangs
          val bangs = Path().apply {
            moveTo(w * 0.33f, h * 0.37f)
            quadraticTo(w * 0.36f, h * 0.24f, w * 0.50f, h * 0.24f)
            quadraticTo(w * 0.64f, h * 0.24f, w * 0.67f, h * 0.37f)
            quadraticTo(w * 0.57f, h * 0.31f, w * 0.50f, h * 0.33f)
            quadraticTo(w * 0.42f, h * 0.31f, w * 0.33f, h * 0.37f)
            close()
          }
          drawPath(path = bangs, color = hair)
        } else {
          // Male head & short hair
          drawOval(
            color = skin,
            topLeft = Offset(w * 0.33f, h * 0.26f),
            size = Size(w * 0.34f, h * 0.37f),
          )
          val topHair = Path().apply {
            moveTo(w * 0.32f, h * 0.39f)
            quadraticTo(w * 0.32f, h * 0.19f, w * 0.50f, h * 0.19f)
            quadraticTo(w * 0.68f, h * 0.19f, w * 0.68f, h * 0.39f)
            quadraticTo(w * 0.64f, h * 0.30f, w * 0.50f, h * 0.30f)
            quadraticTo(w * 0.36f, h * 0.30f, w * 0.32f, h * 0.39f)
            close()
          }
          drawPath(path = topHair, color = hair)
        }

        // Minimal calm eyes & soft smile
        drawCircle(
          color = HairDark,
          radius = w * 0.022f,
          center = Offset(w * 0.43f, h * 0.44f),
        )
        drawCircle(
          color = HairDark,
          radius = w * 0.022f,
          center = Offset(w * 0.57f, h * 0.44f),
        )
        drawArc(
          color = Color(0xFF7D5A4A),
          startAngle = 20f,
          sweepAngle = 140f,
          useCenter = false,
          topLeft = Offset(w * 0.44f, h * 0.48f),
          size = Size(w * 0.12f, h * 0.07f),
          style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.022f),
        )
      }
    }
  }
}

private fun decodeDataUrlToImageBitmap(dataUrl: String?): ImageBitmap? {
  if (dataUrl.isNullOrBlank()) return null
  return try {
    val base64Part = dataUrl.substringAfter("base64,", dataUrl)
    val bytes = Base64.decode(base64Part, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
  } catch (_: Exception) {
    null
  }
}
