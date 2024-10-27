package com.example.ProjectThree

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ProjectThree.ui.theme.ProjectThreeTheme
import android.telephony.SmsManager

class MainActivity : ComponentActivity() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = DatabaseHelper(this, null)
        enableEdgeToEdge()
        setContent {
            ProjectThreeTheme {
                AppNav(dbHelper)
            }
        }
    }
}

@Composable
fun AppNav(dbHelper: DatabaseHelper) {
    //NavController manages navigation between the composable screens
    val navController = rememberNavController()

    //Set up navigation with login, database and SMS permissions screens
    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(navController, dbHelper) }
        composable("database") { DatabaseScreen(navController, dbHelper) }
        composable("sms") { SmsNotificationScreen(navController) }
    }
}

@Composable
fun LoginScreen(navController: NavController, dbHelper: DatabaseHelper) {
    // State variables for user input
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginMessage by remember { mutableStateOf("") }

    // Column Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Username Input Field
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Password Input Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

        Text(loginMessage, color = MaterialTheme.colorScheme.error)

        // Login Button
        Button(
            onClick = {
                val userCursor = dbHelper.getUser(username, password)
                if (userCursor != null && userCursor.count > 0){
                    navController.navigate("database")
                } else {
                    loginMessage = "Invalid Credentials or Create a New Account"
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text("Log In")
        }

        // Create New Account Button
        Button(
            onClick = {
                // Add new user to the Database
                val userId = dbHelper.addUser(username, password)
                if (userId > -1)
                    loginMessage = "Account Created"
                else
                    loginMessage = "Error Creating Account"
            },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text("Create New Account")
        }
    }
}

fun convertCursorToArray(cursor: Cursor): MutableList<String> {
    val getColumnsCount :Int = cursor.columnCount
    var cursorArray :Array<Array<String>> = arrayOf<Array<String>>()
    var columns :Array<String> =  arrayOf<String>()
    var indexes = 0
    cursor.count
    if(cursor.count != 0){
        if(cursor.count == 1){
            cursor.moveToFirst()
            for (i in 0 until getColumnsCount step 1){
                val list: MutableList<String> = columns.toMutableList()
                list.add(cursor.getString(i))
                columns = list.toTypedArray()
            }
            val list: MutableList<Array<String>> = cursorArray.toMutableList()
            list.add(columns)
            cursorArray = list.toTypedArray()
        }else{
            while (cursor.moveToNext()){
                for (i in 0 until getColumnsCount step 1){
                    val list: MutableList<String> = columns.toMutableList()
                    list.add(cursor.getString(i))
                    columns = list.toTypedArray()
                }
                val list: MutableList<Array<String>> = cursorArray.toMutableList()
                list.add(columns)
                cursorArray = list.toTypedArray()
                columns = arrayOf()
                indexes++
            }
        }
    }
    return mutableListOf(cursorArray[0].toString())
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseScreen(navController: NavController, dbHelper: DatabaseHelper) {
    var eventName by remember { mutableStateOf("") }
    var eventDate by remember { mutableStateOf("") }
    var eventTime by remember { mutableStateOf("") }
    var events by remember { mutableStateOf(listOf<Events>()) }
    var eventToEdit by remember { mutableStateOf<Events?>(null) }
    var isEditing by remember { mutableStateOf(false) }



    fun fetchEvents(): MutableList<Events> {
        var eventList = mutableListOf<Events>()
        val cursor = dbHelper.getAllEvents()
        if (cursor != null && cursor.count > 0){
            cursor.moveToFirst()
            do {
                val id = cursor.getInt(cursor.run { getColumnIndex("event_id") })
                val name = cursor.getString(cursor.run { cursor.getColumnIndex("event_name") })
                val date = cursor.getString(cursor.run { cursor.getColumnIndex("event_date") })
                val time = cursor.getString(cursor.run { cursor.getColumnIndex("event_time") })
                eventList.add(Events(id, name, date, time))
            } while (cursor.moveToNext())
            cursor.close()
        }

        return eventList
    }

    // Fetch events when the screen is first displayed
    LaunchedEffect(Unit) {
        events = fetchEvents()
    }

    fun saveOrUpdate(){
        if (isEditing){
            eventToEdit.let {
                if (it != null) {
                    dbHelper.updateEvent(it.id, eventName, eventDate, eventTime)
                    events = fetchEvents()
                    isEditing = false
                }
            }
        } else {
            // Add event to the database
            dbHelper.addEvent(eventName, eventDate, eventTime)

            // Clear input fields
            eventName = ""
            eventDate = ""
            eventTime = ""

            // Refresh the events list
            events = fetchEvents()
        }

        // Clear input fields
        eventName = ""
        eventDate = ""
        eventTime = ""
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Events") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (eventName.isNotEmpty() && eventDate.isNotEmpty() && eventTime.isNotEmpty()) {
                    saveOrUpdate()
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Event")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Button(
                    onClick = {
                        navController.navigate("sms")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) { Text("Send SMS Notification")}
                // Input fields for new event creation
                OutlinedTextField(
                    value = eventName,
                    onValueChange = { eventName = it },
                    label = { Text("Event Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = eventDate,
                    onValueChange = { eventDate = it },
                    label = { Text("Event Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = eventTime,
                    onValueChange = { eventTime = it },
                    label = { Text("Event Time (HH:MM)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Display the grid of events using forEach
                if (events.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(events) {
                            event ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Column (
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ){
                                    Text(text = "Name: ${event.name}", style = MaterialTheme.typography.bodyLarge)
                                    Text(text = "Date: ${event.date}", style = MaterialTheme.typography.bodyMedium)
                                    Text(text = "Time: ${event.time}", style = MaterialTheme.typography.bodySmall)

                                    Spacer(modifier = Modifier.height(8.dp))

                                    //Edit button
                                    Button(onClick = {
                                        eventToEdit = event
                                        eventName = event.name
                                        eventDate = event.date
                                        eventTime = event.time
                                        isEditing = true
                                    }) { Text("Edit") }

                                    //Delete button
                                    Button(onClick = {
                                        dbHelper.deleteEvent(event.id)
                                        events = fetchEvents()
                                    }) { Text("Delete")}

                                }
                            }
                        }
                    }
                } else {
                    // If there are no events, show this text
                    Text(text = "No events available.")
                }
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsNotificationScreen(navController: NavController) {
    val context = LocalContext.current
    val smsPermissionGranted = remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("") } // Input for recipient phone number
    var messageText by remember { mutableStateOf("Event Reminder: Don't forget!") } // Default SMS text

    // SMS Permission launcher
    val requestSmsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        smsPermissionGranted.value = isGranted
        if (!isGranted) {
            Toast.makeText(context, "SMS permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Check if SMS permission has been granted
    LaunchedEffect(Unit) {
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        smsPermissionGranted.value = isGranted
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("SMS Notifications") }) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (smsPermissionGranted.value) {
                    Text("SMS permission granted. You can send SMS notifications.")

                    // Input field for recipient's phone number
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Recipient Phone Number") },
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    )

                    // Input field for SMS message content
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        label = { Text("SMS Message") },
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    )

                    // Send SMS Button
                    Button(onClick = {
                        sendSms(context, phoneNumber, messageText)
                    }) {
                        Text("Send SMS")
                    }
                } else {
                    Text("SMS permission not granted.")

                    // Button to request SMS permissions
                    Button(onClick = {
                        requestSmsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                    }) {
                        Text("Enable SMS Notifications")
                    }
                }
            }
        }
    }
}

// Function to send an SMS
fun sendSms(context: android.content.Context, phoneNumber: String, message: String) {
    if (phoneNumber.isNotEmpty()) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Toast.makeText(context, "SMS sent to $phoneNumber", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "SMS failed to send: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    } else {
        Toast.makeText(context, "Please enter a phone number", Toast.LENGTH_SHORT).show()
    }
}