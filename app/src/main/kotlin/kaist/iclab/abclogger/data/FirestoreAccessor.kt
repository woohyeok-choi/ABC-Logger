package kaist.iclab.abclogger.data

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.*
import java.util.*

object FirestoreAccessor {
    data class SubjectData(var uuid: String? = null) {
        fun toMap(): Map<String, Any?> {
            return mapOf(
                SubjectData::uuid.name to uuid
            ).filterValues { it != null }
        }
    }

    data class ExperimentData(var lastTimeSurveyTriggered: Long? = null) {
        fun toMap(): Map<String, Any?> {
            return mapOf(
                ExperimentData::lastTimeSurveyTriggered.name to lastTimeSurveyTriggered
            ).filterValues { it != null }
        }
    }

    private const val COLLECTION_SUBJECT = "subjects"
    private const val COLLECTION_EXPERIMENT = "experiments"
    private var snapshotListener: ListenerRegistration? = null

    private fun getSubjectReference(subjectEmail: String) : DocumentReference {
        return FirebaseFirestore.getInstance()
            .collection(COLLECTION_SUBJECT)
            .document(subjectEmail)
    }

    private fun getExperimentReference(subjectEmail: String, experimentUuid: String) : DocumentReference {
        return FirebaseFirestore.getInstance()
            .collection(COLLECTION_SUBJECT)
            .document(subjectEmail)
            .collection(COLLECTION_EXPERIMENT)
            .document(experimentUuid)
    }

    private fun toExperimentData(map: Map<String, Any>?) : ExperimentData {
        return ExperimentData().apply {
            ExperimentData::lastTimeSurveyTriggered.set(this, map?.get(ExperimentData::lastTimeSurveyTriggered.name)?.toString()?.toLong() ?: Long.MIN_VALUE)
        }
    }

    private fun toSubjectData(map: Map<String, Any>?) : SubjectData {
        return SubjectData().apply {
            SubjectData::uuid.set(this, map?.get(SubjectData::uuid.name)?.toString() ?: UUID.randomUUID().toString())
        }
    }

    fun setOrUpdate(subjectEmail: String, experimentUuid: String, data: ExperimentData) : Task<Void> {
        val ref = getExperimentReference(subjectEmail, experimentUuid)
        return ref.get().onSuccessTask {
                return@onSuccessTask if(it?.data == null) { ref.set(data.toMap(), SetOptions.merge()) } else { ref.update(data.toMap()) }
            }
    }

    fun get(subjectEmail: String, experimentUuid: String) : Task<ExperimentData?> {
        return getExperimentReference(subjectEmail, experimentUuid).get()
            .continueWith {
                if(it.result.data != null) toExperimentData(it.result.data) else null
            }
    }

    fun delete(subjectEmail: String, experimentUuid: String) : Task<Void> {
        return getExperimentReference(subjectEmail, experimentUuid).delete()
    }

    fun get(subjectEmail: String) : Task<SubjectData?> {
        return getSubjectReference(subjectEmail).get().continueWith {
            if(it.result.data != null) toSubjectData(it.result.data) else null
        }
    }

    fun setOrUpdate(subjectEmail: String, data: SubjectData) : Task<Void> {
        val ref = getSubjectReference(subjectEmail)
        return ref.get().onSuccessTask {
            return@onSuccessTask if(it?.data == null) { ref.set(data.toMap(), SetOptions.merge()) } else { ref.update(data.toMap()) }
        }
    }

    fun set(subjectEmail: String, data: SubjectData) : Task<Void> {
        return getSubjectReference(subjectEmail).set(data.toMap(), SetOptions.merge())
    }

    fun update(subjectEmail: String, data: SubjectData) : Task<Void> {
        return getSubjectReference(subjectEmail).update(data.toMap())
    }

    fun delete(subjectEmail: String) : Task<Void> {
        return getSubjectReference(subjectEmail).delete()
    }

    fun listenSubjectDataChange(subjectEmail: String, listener: (data: SubjectData) -> Unit) {
        snapshotListener?.remove()
        snapshotListener = getSubjectReference(subjectEmail).addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(firebaseFirestoreException != null && documentSnapshot != null) {
                listener(toSubjectData(documentSnapshot.data))
            }
        }
    }
}