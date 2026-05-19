/**
 * @file LandingScreen.kt
 * @description Landing and authentication view with custom grid background and text inputs
 * @module frontend/ui
 */

package com.example.hevcapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GridBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val cellSize = 24.dp.toPx()
        val gridColor = Color(0xFFE8E8E8)
        val strokeWidth = 1.dp.toPx()

        var x = 0f
        while (x < size.width) {
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = strokeWidth
            )
            x += cellSize
        }

        var y = 0f
        while (y < size.height) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = strokeWidth
            )
            y += cellSize
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LandingScreen(
    onConnectInitiated: (roomName: String, participantName: String) -> Unit,
    externalError: String? = null,
    modifier: Modifier = Modifier
) {
    var roomName by remember { mutableStateOf("") }
    var participantName by remember { mutableStateOf("") }
    var localErrorMessage by remember { mutableStateOf<String?>(null) }
    val errorMessage = localErrorMessage ?: externalError
    
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Technical Grid Background
        GridBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.MeetingRoom,
                        contentDescription = "App Logo",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MEDIACORE",
                        style = TextStyle(
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = " V4.0",
                        style = TextStyle(
                            color = Color.Gray,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = Color.DarkGray,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFF0F0F0), shape = RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFFD0D0D0), shape = RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Profile",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.15f))

            // Title & Subtitle block with Left Accent Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Black vertical line (thickness 4dp)
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(56.dp)
                        .background(Color.Black)
                )
                
                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "HD VIDEO CALL",
                        style = TextStyle(
                            color = Color.Black,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = "HIGH-BITRATE SECURE LINK • ENCRYPTED",
                        style = TextStyle(
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // White Input Card (No rounding, sharp technical outline)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .border(1.dp, Color(0xFFE0E0E0), shape = RoundedCornerShape(0.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(0.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // ROOM ENVIRONMENT Field
                    Text(
                        text = "ROOM ENVIRONMENT",
                        style = TextStyle(
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomSharpInput(
                        value = roomName,
                        onValueChange = {
                            if (it.length <= 50) roomName = it.replace(Regex("[^a-zA-Z0-9_-]"), "")
                        },
                        placeholder = "ENTER ROOM ID",
                        icon = Icons.Filled.MeetingRoom,
                        imeAction = ImeAction.Next
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // OPERATOR IDENTITY Field
                    Text(
                        text = "OPERATOR IDENTITY",
                        style = TextStyle(
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomSharpInput(
                        value = participantName,
                        onValueChange = {
                            if (it.length <= 50) participantName = it.replace(Regex("[^a-zA-Z0-9_-]"), "")
                        },
                        placeholder = "OPERATOR NAME",
                        icon = Icons.Filled.Badge,
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions(onDone = {
                            keyboardController?.hide()
                        })
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Error Message Banner
                    errorMessage?.let {
                        Text(
                            text = it.uppercase(),
                            color = Color.Red,
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    // INITIALIZE CONNECTION Button
                    Button(
                        onClick = {
                            if (roomName.trim().length < 3) {
                                localErrorMessage = "Room ID must be at least 3 characters"
                                return@Button
                            }
                            if (participantName.trim().length < 2) {
                                localErrorMessage = "Operator Name must be at least 2 characters"
                                return@Button
                            }
                            localErrorMessage = null
                            onConnectInitiated(roomName.trim(), participantName.trim())
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "INITIALIZE CONNECTION",
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Filled.ArrowForward,
                                contentDescription = "Next arrow",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.4f))
        }
    }
}

@Composable
fun CustomSharpInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    imeAction: ImeAction,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(1.dp, Color(0xFFDCDCDC), shape = RoundedCornerShape(0.dp))
            .background(Color.White)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = TextStyle(
                            color = Color(0xFFAAAAAA),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(Color.Black),
                    keyboardOptions = KeyboardOptions(imeAction = imeAction),
                    keyboardActions = keyboardActions,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = placeholder,
                tint = Color(0xFF888888),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
