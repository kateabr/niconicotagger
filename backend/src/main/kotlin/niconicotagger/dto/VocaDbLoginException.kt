package niconicotagger.dto

import niconicotagger.dto.inner.vocadb.DbLoginError

class VocaDbLoginException(val errorData: DbLoginError) : RuntimeException(errorData.toString())
