package org.openedx.app.room

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database Migrations
 *
 * This file contains all database migrations for the app.
 * When adding new migrations:
 * 1. Create a new MIGRATION_X_Y object
 * 2. Add it to the ALL_MIGRATIONS array
 * 3. Update DATABASE_VERSION in AppDatabase.kt
 *
 * Best Practices:
 * - Never remove migrations that have been released
 * - Keep migrations in chronological order
 * - Use descriptive comments
 * - Test migrations thoroughly before release
 */

/**
 * Migration from version 1 to version 2
 *
 * Changes:
 * - Adds new fields to course_discovery_table:
 *   - courseRequirement: Course prerequisites/requirements
 *   - description: Detailed course description
 *   - learningOutcomes: Expected learning outcomes
 *   - instructors: JSON string of instructor information
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add new columns to course_discovery_table with default empty string values
        db.execSQL("ALTER TABLE course_discovery_table ADD COLUMN courseRequirement TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE course_discovery_table ADD COLUMN description TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE course_discovery_table ADD COLUMN learningOutcomes TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE course_discovery_table ADD COLUMN instructors TEXT NOT NULL DEFAULT ''")
    }
}

/**
 * Migration from version 2 to version 3
 *
 * Changes:
 * - Adds new field to course_discovery_table:
 *   - purchaseLink: URL to purchase paid courses
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add purchaseLink column to course_discovery_table (nullable field)
        db.execSQL("ALTER TABLE course_discovery_table ADD COLUMN purchaseLink TEXT DEFAULT NULL")
    }
}

/**
 * All migrations in chronological order.
 * This array is used by the database builder to apply migrations automatically.
 *
 * To add a new migration:
 * - Create a new MIGRATION_X_Y object above
 * - Add it to this array: arrayOf(MIGRATION_1_2, MIGRATION_2_3, ...)
 */
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3
    // Add future migrations here, e.g.:
    // MIGRATION_3_4,
    // MIGRATION_4_5,
)

