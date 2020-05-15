package de.develappers.facerecognition.database.repository
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.microsoft.projectoxford.face.contract.Candidate
import com.microsoft.projectoxford.face.contract.IdentifyResult
import de.develappers.facerecognition.database.dao.VisitorDao
import de.develappers.facerecognition.database.model.Visitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

// Declares the DAO as a private property in the constructor. Pass in the DAO
// instead of the whole database, because you only need access to the DAO
class VisitorRepository(private val visitorDao: VisitorDao) {

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    //TODO: get only selected visitors - the ones, that match ids returned by AI services as the most likely persons
    var candidatesList = mutableListOf<Candidate>()
    var candidates = MutableLiveData<List<Candidate>>()
    val allVisitors : LiveData<List<Visitor>> = visitorDao.getAll()

    suspend fun insert(visitor: Visitor) {
        visitorDao.insert(visitor)
    }

    suspend fun findVisitorByMicrosoftId(personId: String): Visitor  {
        return visitorDao.findByMicrosoftId(personId)
    }

    fun addCandidateToSelection(visitor: Visitor) {
        selectedVisitorsList.add(visitor)
        selectedVisitors.value = selectedVisitorsList
    }


}