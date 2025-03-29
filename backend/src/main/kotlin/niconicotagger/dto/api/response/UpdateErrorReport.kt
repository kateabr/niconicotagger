package niconicotagger.dto.api.response

interface UpdateReport

class UpdateSuccess : UpdateReport

data class UpdateError(val entryId: Long, val message: String?) : UpdateReport
