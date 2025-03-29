package niconicotagger.dto.inner.vocadb

import niconicotagger.dto.inner.misc.UserGroup
import niconicotagger.dto.inner.misc.UserGroup.Admin
import niconicotagger.dto.inner.misc.UserGroup.Moderator
import niconicotagger.dto.inner.misc.UserGroup.Trusted

data class VocaDbUser(val active: Boolean, val groupId: UserGroup) {
    private val allowedGroups = setOf(Trusted, Moderator, Admin)

    fun checkUserPermissions() {
        if (!active) error("User is inactive")
        if (!allowedGroups.contains(groupId)) error("User group $groupId is not allowed")
    }
}
