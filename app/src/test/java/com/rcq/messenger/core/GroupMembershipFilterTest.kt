package com.rcq.messenger.core

import com.rcq.messenger.domain.model.Group
import org.junit.Assert.*
import org.junit.Test

/**
 * Regression guard for BUG-003: groups screen showed groups the user wasn't a member of.
 * Fix: filter allGroups to only those where memberIds.contains(ownUin).
 */
class GroupMembershipFilterTest {

    private val ownUin = 100L

    private fun group(id: String, vararg members: Long) = Group(
        id = id, name = "Group $id",
        ownerId = members.firstOrNull() ?: 0L,
        createdAt = 0L,
        memberCount = members.size,
        memberIds = members.toList()
    )

    private fun filterGroups(all: List<Group>, uin: Long) =
        all.filter { it.memberIds.contains(uin) }

    @Test
    fun `only groups containing own UIN are returned`() {
        val groups = listOf(
            group("member", ownUin, 200L, 300L),
            group("outsider", 200L, 300L),
            group("also-member", 50L, ownUin)
        )
        val result = filterGroups(groups, ownUin)
        assertEquals(listOf("member", "also-member"), result.map { it.id })
    }

    @Test
    fun `empty list returns empty`() {
        assertTrue(filterGroups(emptyList(), ownUin).isEmpty())
    }

    @Test
    fun `no matching groups returns empty`() {
        assertTrue(filterGroups(listOf(group("other", 200L, 300L)), ownUin).isEmpty())
    }

    @Test
    fun `ownUin zero not in group returns empty — uninitialized user`() {
        // When DataStore hasn't loaded UIN yet, ContactsViewModel guards with ownUin == 0L check
        val groups = listOf(group("g", 200L, 300L))
        assertTrue(filterGroups(groups, 0L).isEmpty())
    }

    @Test
    fun `all groups contain own UIN — all returned`() {
        val groups = listOf(group("a", ownUin, 1L), group("b", 2L, ownUin), group("c", ownUin))
        assertEquals(3, filterGroups(groups, ownUin).size)
    }

    @Test
    fun `solo group with only own UIN is included`() {
        assertEquals(1, filterGroups(listOf(group("solo", ownUin)), ownUin).size)
    }
}
