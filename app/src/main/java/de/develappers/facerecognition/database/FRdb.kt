package de.develappers.facerecognition.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import de.develappers.facerecognition.FaceApp
import de.develappers.facerecognition.database.dao.CompanyDao
import de.develappers.facerecognition.database.dao.LogDao
import de.develappers.facerecognition.database.dao.VisitorDao
import de.develappers.facerecognition.database.model.Company
import de.develappers.facerecognition.database.model.LogEntry
import de.develappers.facerecognition.database.model.Visitor
import de.develappers.facerecognition.serviceAI.ImageHelper
import de.develappers.facerecognition.serviceAI.MicrosoftServiceAI
import de.develappers.facerecognition.utils.VISITORS_GROUP_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

@Database(entities = arrayOf(Visitor::class, Company::class, LogEntry::class), version = 1)
@TypeConverters(Converters::class)
abstract class FRdb : RoomDatabase() {
    abstract fun visitorDao(): VisitorDao
    abstract fun companyDao(): CompanyDao
    abstract fun logDao(): LogDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: FRdb? = null

        fun getDatabase(context: Context, scope: CoroutineScope): FRdb {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FRdb::class.java,
                    "FRdb"
                )
                    .addCallback(FRdbCallback(scope, context))
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }

    private class FRdbCallback(private val scope: CoroutineScope, private val context: Context) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch { populateDatabase(database.visitorDao())}
                }
            }

        suspend fun populateDatabase(visitorDao: VisitorDao) {
            // Delete all content here.
            visitorDao.deleteAll()

            // Add sample company
            val company = Company("apple")
            val galleryFolder = FaceApp.galleryFolder

            //access microsoftAI Interface
            val microsoftServiceAI = MicrosoftServiceAI(context)
            microsoftServiceAI.microsoftDeletePersonGroup(VISITORS_GROUP_ID)
            microsoftServiceAI.addPersonGroup()

            val databaseFolder = "database"
            //for every person in database get the first image and store the path in local database
            context.assets.list(databaseFolder)?.forEachIndexed {index, element ->
                val file: File = ImageHelper.saveVisitorPhotoLocally(context, element, galleryFolder, databaseFolder)
                println(element.toString())
                //wait, because only 20TPM in free tier
                TimeUnit.SECONDS.sleep(2L)
                Log.d("IMG to retrieve", file.path)
                val servicePersonId = microsoftServiceAI.addNewVisitorToDatabase(VISITORS_GROUP_ID, file.path)
                Log.d("ServicePersonId", servicePersonId)
                val visitor = Visitor(
                    "Visitor",
                    element.toString(),
                    company,
                    true
                )
                visitor.imgPaths.add(file.path)
                visitor.microsoftId = servicePersonId
                visitorDao.insert(visitor)
            }
            Log.d("Database", "populated")
            microsoftServiceAI.microsoftTrainPersonGroup(VISITORS_GROUP_ID)

        }

        }
    }
