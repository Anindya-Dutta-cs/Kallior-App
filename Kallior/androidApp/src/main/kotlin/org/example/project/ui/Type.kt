package org.example.project.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.example.project.R

val Philosopher = FontFamily(
    Font(R.font.philosopher_regular, FontWeight.Normal),
    Font(R.font.philosopher_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.philosopher_bold, FontWeight.Bold),
    Font(R.font.philosopher_bold_italic, FontWeight.Bold, FontStyle.Italic)
)
