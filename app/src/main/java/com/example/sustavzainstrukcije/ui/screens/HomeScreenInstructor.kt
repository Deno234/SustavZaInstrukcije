package com.example.sustavzainstrukcije.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun HomeScreenInstructor(navController: NavHostController, instructorId: String?) {
    Log.d("HomeScreenInstructor", "Instructor id: $instructorId")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        Button(
            onClick = { navController.navigate("profile") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("My Profile")
        }

        Button(
            onClick = { navController.navigate("appointments") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("My Appointments")
        }

        Button(
            onClick = { navController.navigate("messages/$instructorId") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("My Messages")
        }

        Button(
            onClick = { navController.navigate("otherInstructors") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Other Instructors")
        }
    }
}