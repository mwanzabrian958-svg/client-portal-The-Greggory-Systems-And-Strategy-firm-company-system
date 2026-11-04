package com.greggory.portal.data.local

import com.greggory.portal.BuildConfig

/**
 * Secondary Primary Database Configuration (Aiven MySQL Cloud Database)
 * 
 * Wired as the secondary/failover primary database connection endpoint:
 * - Host: mysql-57dd84d-the-greggory-systems-and-strategy-firm.i.aivencloud.com
 * - Port: 27809
 * - Database: defaultdb
 * - User: avnadmin
 * - SSL Mode: REQUIRED
 */
object SecondaryDatabaseConfig {
    val DB_HOST: String = BuildConfig.SECONDARY_DB_HOST
    val DB_PORT: Int = 27809
    val DB_NAME: String = BuildConfig.SECONDARY_DB_NAME
    val DB_USER: String = BuildConfig.SECONDARY_DB_USER
    val DB_PASSWORD: String = BuildConfig.SECONDARY_DB_PASSWORD
    val DB_URI: String = BuildConfig.SECONDARY_DB_URI
    val SSL_MODE: String = "REQUIRED"

    fun getConnectionSummary(): String {
        return "Aiven MySQL Secondary Primary DB [Host: $DB_HOST:$DB_PORT, Database: $DB_NAME, SSL: $SSL_MODE]"
    }
}
