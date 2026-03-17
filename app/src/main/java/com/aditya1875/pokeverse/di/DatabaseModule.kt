package com.aditya1875.pokeverse.di

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aditya1875.pokeverse.data.local.PokemonDatabase
import com.aditya1875.pokeverse.data.local.TeamDatabase
import com.aditya1875.pokeverse.data.local.dao.TeamDao
import com.aditya1875.pokeverse.data.local.entity.TeamEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.dsl.module
import java.util.UUID

val databaseModule = module {

    single {
        CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    val migration23 = object : Migration(2, 3) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS teams (
                    teamId TEXT PRIMARY KEY NOT NULL,
                    teamName TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    isDefault INTEGER NOT NULL DEFAULT 0
                )
                """
            )

            val defaultTeamId = UUID.randomUUID().toString()

            db.execSQL(
                """
                INSERT INTO teams (teamId, teamName, createdAt, isDefault)
                VALUES ('$defaultTeamId', 'My Team', ${System.currentTimeMillis()}, 1)
                """
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS team_members_new (
                    id TEXT PRIMARY KEY NOT NULL,
                    teamId TEXT NOT NULL,
                    name TEXT NOT NULL,
                    imageUrl TEXT NOT NULL,
                    addedAt INTEGER NOT NULL,
                    FOREIGN KEY(teamId) REFERENCES teams(teamId) ON DELETE CASCADE
                )
                """
            )

            db.execSQL(
                """
                INSERT INTO team_members_new (id, teamId, name, imageUrl, addedAt)
                SELECT 
                    LOWER(HEX(RANDOMBLOB(16))),
                    '$defaultTeamId',
                    name,
                    imageUrl,
                    ${System.currentTimeMillis()}
                FROM team_members
                """
            )

            db.execSQL("DROP TABLE team_members")

            db.execSQL("ALTER TABLE team_members_new RENAME TO team_members")

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_team_members_teamId
                ON team_members(teamId)
                """
            )
        }
    }

    single {

        Room.databaseBuilder(
            get(),
            TeamDatabase::class.java,
            "pokeverseTeam_db"
        )
            .addMigrations(migration23)
            .addCallback(object : RoomDatabase.Callback() {

                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)

                    get<CoroutineScope>().launch {

                        get<TeamDao>().createTeam(
                            TeamEntity(
                                teamName = "My Team",
                                isDefault = true
                            )
                        )

                    }
                }

            })
            .fallbackToDestructiveMigration(false)
            .build()
    }

    single { get<TeamDatabase>().teamDao() }
    single { get<TeamDatabase>().favoritesDao() }
    single { get<TeamDatabase>().gameScoreDao() }

    single {

        Room.databaseBuilder(
            get(),
            PokemonDatabase::class.java,
            "pokemon_db"
        ).build()

    }

    single { get<PokemonDatabase>().pokemonDao() }

}