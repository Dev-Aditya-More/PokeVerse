package com.example.pokeverse.ui.theme

import android.content.Context
import androidx.compose.material3.Typography
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.GoogleFont.Provider
import com.example.pokeverse.R

@Composable
fun appTypography(context: Context): Typography {
    val provider = Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs
    )

    val sora = FontFamily(
        Font(googleFont = GoogleFont("Sora"), fontProvider = provider)
    )
    val inter = FontFamily(
        Font(googleFont = GoogleFont("Inter"), fontProvider = provider)
    )

    return Typography(
        displayLarge = TextStyle(fontFamily = sora, fontSize = 32.sp),
        headlineSmall = TextStyle(fontFamily = sora, fontSize = 24.sp),
        titleMedium = TextStyle(fontFamily = inter, fontSize = 18.sp),
        bodyMedium = TextStyle(fontFamily = inter, fontSize = 16.sp),
        labelSmall = TextStyle(fontFamily = inter, fontSize = 12.sp)
    )
}
