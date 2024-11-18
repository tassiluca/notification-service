package io.github.positionpal.notification.application.groups

import io.github.positionpal.entities.GroupId
import io.github.positionpal.entities.UserId

/** A repository to manage groups and their members. */
interface GroupsRepository {

    /** Adds the given [userId] to the group with the specified [groupId]. */
    suspend fun addMember(groupId: GroupId, userId: UserId): Result<Unit>

    /** Removes the given [userId] from the group with the specified [groupId]. */
    suspend fun removeMember(groupId: GroupId, userId: UserId): Result<Unit>

    /** @return the members of the group with the specified [groupId]. */
    suspend fun getMembersOf(groupId: GroupId): Result<Set<UserId>>

    /** @return the groups of the user with the specified [userId]. */
    suspend fun getGroupsOf(userId: UserId): Result<Set<GroupId>>
}
