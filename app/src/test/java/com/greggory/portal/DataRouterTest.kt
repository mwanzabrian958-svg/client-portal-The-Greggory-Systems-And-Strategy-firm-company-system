package com.greggory.portal

import com.greggory.portal.utils.DataRouter
import org.junit.Assert.*
import org.junit.Test

class DataRouterTest {

    @Test
    fun verifyRoutingIntegrity_Matches_ReturnsTrue() {
        val remoteId = 123
        val localId = 123
        assertTrue("Integrity check should pass when IDs match", 
            DataRouter.verifyRoutingIntegrity(remoteId, localId))
    }

    @Test
    fun verifyRoutingIntegrity_Mismatches_ReturnsFalse() {
        val remoteId = 123
        val localId = 456
        assertFalse("Integrity check should fail when IDs mismatch", 
            DataRouter.verifyRoutingIntegrity(remoteId, localId))
    }
}
