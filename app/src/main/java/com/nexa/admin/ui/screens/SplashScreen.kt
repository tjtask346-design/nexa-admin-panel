package com.nexa.admin.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexa.admin.R
import com.nexa.admin.ui.theme.*

@Composable
fun SplashScreen(onDone: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(NexaBg)
            .drawBehind {
                val radius = size.width * 0.75f
                val c = Offset(size.width * 0.5f, size.height * 0.45f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to NexaGreen.copy(alpha = 0.22f),
                            0.55f to NexaGreen.copy(alpha = 0.06f),
                            1.00f to Color.Transparent
                        ),
                        center = c, radius = radius
                    ),
                    radius = radius, center = c
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(36.dp))
            ) {
                Image(
                    painter = painterResource(R.drawable.nexa_logo),
                    contentDescription = "Nexa",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "NEXA",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 8.sp,
                color = NexaGreen
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "ADMIN PANEL",
                color = NexaTeal,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp
            )
        }
    }
}
