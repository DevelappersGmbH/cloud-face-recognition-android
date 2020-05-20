package de.develappers.facerecognition.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.microsoft.projectoxford.face.contract.Candidate
import com.microsoft.projectoxford.face.contract.IdentifyResult
import de.develappers.facerecognition.database.model.RecognisedCandidate
import de.develappers.facerecognition.database.model.Visitor
import de.develappers.facerecognition.database.repository.VisitorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VisitorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VisitorRepository
    // Using LiveData and caching what getAlphabetizedWords returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
    val allVisitors: LiveData<List<Visitor>>
    val candidates: LiveData<List<RecognisedCandidate>>

    init {
        val visitorDao = FRdb.getDatabase(application, viewModelScope).visitorDao()
        repository = VisitorRepository(visitorDao)
        allVisitors = repository.allVisitors
        candidates = repository.candidates
    }

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun insert(visitor: Visitor) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(visitor)
    }

    /*fun getRandomVisitor() : LiveData<Visitor> {
        return repository.getRandomVisitor()
    }*/


    fun findVisitorByMicrosoftId(candidate: Candidate) = viewModelScope.launch(Dispatchers.IO) {
        repository.findVisitorByMicrosoftId(candidate.personId.toString())
    }

    fun updateVisitor(visitor: Visitor) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateVisitor(visitor)
    }
}
