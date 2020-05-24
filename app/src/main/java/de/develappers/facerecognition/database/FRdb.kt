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
import de.develappers.facerecognition.serviceAI.*
import de.develappers.facerecognition.VISITORS_GROUP_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
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

    private class FRdbCallback(private val scope: CoroutineScope, private val context: Context) :
        RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch { populateDatabase(database.visitorDao()) }
            }
        }

        suspend fun populateDatabase(visitorDao: VisitorDao) {
            val innerClient = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.MINUTES) // connect timeout
                .writeTimeout(5, TimeUnit.MINUTES) // write timeout
                .readTimeout(5, TimeUnit.MINUTES) // read timeout
                .build()

            // delete all content here
            visitorDao.deleteAll()

            // add sample company
            val company = Company("apple")
            val galleryFolder = FaceApp.galleryFolder

            ///AI services
            val serviceProviders = ServiceFactory.createAIServices(context, FaceApp.values)

            serviceProviders.forEach{
                if (it.isActive){
                    it.deletePersonGroup(VISITORS_GROUP_ID)
                    it.addPersonGroup(VISITORS_GROUP_ID)
                }
            }

            val databaseFolder = "database"
            //for every person in database get the first image and store the path in local database
            context.assets.list(databaseFolder)?.forEachIndexed { index, element ->
                val file: File = ImageHelper.saveVisitorPhotoLocally(context, element, galleryFolder, databaseFolder)
                println(element.toString())
                //wait, because only 20TPM in Microsoft free tier
                TimeUnit.SECONDS.sleep(2L)
                Log.d("IMG to retrieve", file.path)

                //register face in the services database
                val visitor = Visitor(
                    "Visitor",
                    element.toString(),
                    company,
                    true
                )

                serviceProviders.forEach{
                    if (it.isActive){
                        it.addNewVisitorToDatabase(VISITORS_GROUP_ID, file.path, visitor)
                    }
                }
                visitor.imgPaths.add(file.path)
                visitorDao.insert(visitor)
            }
            Log.d("Database", "populated")

            //train services
            serviceProviders.forEach{
                if (it.isActive){
                    it.train()
                }
            }
        }

    }
}
