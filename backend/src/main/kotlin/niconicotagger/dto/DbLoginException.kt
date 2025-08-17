package niconicotagger.dto

import niconicotagger.dto.inner.vocadb.DbLoginError

class DbLoginException(val errorData: DbLoginError) : RuntimeException(errorData.toString())
