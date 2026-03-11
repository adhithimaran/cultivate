package com.adhithimaran.cultivate.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun HomeScreen(onSignOut: () -> Unit) {
    Column {
        Text("Home Screen — coming soon!")
        Button(onClick = onSignOut) {
            Text("Sign Out")
        }
    }
}