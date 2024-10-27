package com.example.ProjectThree

import android.content.ContentValues
import android.content.Context
import  android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context, factory: SQLiteDatabase.CursorFactory?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {


    override fun onCreate(db: SQLiteDatabase?) {
        Log.d("DatabaseHelper", "onCreate called: Creating user and events tables")

        val createUserTable = ("CREATE TABLE $TABLE_USER (" +
                "$COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_USERNAME TEXT, " +
                "$COLUMN_PASSWORD TEXT)")

        val createEventTable = ("CREATE TABLE $TABLE_EVENTS (" +
                "$COLUMN_EVENT_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_EVENT_NAME TEXT, " +
                "$COLUMN_EVENT_DATE TEXT, " +
                "$COLUMN_EVENT_TIME TEXT)")

        db?.execSQL(createUserTable)
        db?.execSQL(createEventTable)

        Log.d("DatabaseHelper", "Tables created successfully")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USER")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_EVENTS")
        onCreate(db)
    }

    //CRUD operations

    //Create user
    fun addUser(username: String, password: String): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_USERNAME, username)
            put(COLUMN_PASSWORD, password)
        }
        return db.insert(TABLE_USER, null, contentValues)
    }

    //Read user
    fun getUser(username: String, password: String): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery(
            "SELECT * FROM $TABLE_USER WHERE $COLUMN_USERNAME=? AND $COLUMN_PASSWORD=?",
            arrayOf(username, password),
            null
        )
    }

    // Create Event
    fun addEvent(name: String, date: String, time: String): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_EVENT_NAME, name)
            put(COLUMN_EVENT_DATE, date)
            put(COLUMN_EVENT_TIME, time)
        }
        return db.insert(TABLE_EVENTS, null, contentValues)
    }

    fun getAllEvents(): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_EVENTS", null)
    }

    //update event
    fun updateEvent(eventId: Int, eventName: String, eventDate: String, eventTime: String) {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_EVENT_DATE, eventDate)
            put(COLUMN_EVENT_NAME, eventName)
            put(COLUMN_EVENT_TIME, eventTime)
        }
        db.update(TABLE_EVENTS, contentValues, "$COLUMN_EVENT_ID=?", arrayOf(eventId.toString()))
    }


    //delete item
    fun deleteItem(eventId: Int) {
        val db = this.writableDatabase
        db.delete(TABLE_EVENTS, "$COLUMN_EVENT_ID=?", arrayOf(eventId.toString()))
    }

    fun deleteEvent(eventId: Int): Int {
        val db = this.writableDatabase
        return db.delete(TABLE_EVENTS, "$COLUMN_EVENT_ID=?", arrayOf(eventId.toString()))
    }

    companion object {
        private const val DATABASE_NAME = "project.db"
        private const val DATABASE_VERSION = 3

        // User table
        const val TABLE_USER = "user"
        val COLUMN_USER_ID = "id"
        val COLUMN_USERNAME = "username"
        val COLUMN_PASSWORD = "password"

        // Event table
        const val TABLE_EVENTS = "Events"
        const val COLUMN_EVENT_ID = "event_id"
        val COLUMN_EVENT_NAME = "event_name"
        val COLUMN_EVENT_DATE = "event_date"
        val COLUMN_EVENT_TIME = "event_time"
    }
}

