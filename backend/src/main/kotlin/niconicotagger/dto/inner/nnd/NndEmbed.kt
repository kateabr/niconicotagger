package niconicotagger.dto.inner.nnd

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.DEDUCTION

@JsonTypeInfo(use = DEDUCTION) sealed interface NndEmbed
