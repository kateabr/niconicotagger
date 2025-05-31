package niconicotagger.dto.api.response

interface UpdateReport {
    val entryId: Long
}

class UpdateSuccess : UpdateReport {
    override val entryId: Long = -1
}

data class UpdateError(override val entryId: Long, val message: String?) : UpdateReport
