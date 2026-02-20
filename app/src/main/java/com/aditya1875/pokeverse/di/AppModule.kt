package com.aditya1875.pokeverse.di

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aditya1875.pokeverse.data.local.TeamDatabase
import com.aditya1875.pokeverse.data.local.PokemonDatabase
import com.aditya1875.pokeverse.data.local.dao.TeamDao
import com.aditya1875.pokeverse.data.local.entity.TeamEntity
import com.aditya1875.pokeverse.data.preferences.ThemePreferences
import org.koin.androidx.viewmodel.dsl.viewModel
import com.aditya1875.pokeverse.data.remote.PokeApi
import com.aditya1875.pokeverse.data.repository.PokemonRepoImpl
import com.aditya1875.pokeverse.domain.repository.DescriptionRepo
import com.aditya1875.pokeverse.domain.repository.PokemonRepo
import com.aditya1875.pokeverse.domain.repository.PokemonSearchRepository
import com.aditya1875.pokeverse.presentation.ui.viewmodel.MatchViewModel
import com.aditya1875.pokeverse.presentation.ui.viewmodel.PokeGuessViewModel
import com.aditya1875.pokeverse.presentation.ui.viewmodel.PokemonViewModel
import com.aditya1875.pokeverse.presentation.ui.viewmodel.QuizViewModel
import com.aditya1875.pokeverse.presentation.ui.viewmodel.SettingsViewModel
import com.aditya1875.pokeverse.utils.SoundManager
import com.aditya1875.pokeverse.utils.TeamMapper
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID

val appModule = module {

    includes(
        billingModule
    )

    // Retrofit for PokéAPI
    single(named("pokeapi")) {
        Retrofit.Builder()
            .baseUrl("https://pokeapi.co/api/v2/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // API Services
    single<PokeApi> {
        get<Retrofit>(named("pokeapi")).create(PokeApi::class.java)
    }

    // Repos
    single<PokemonRepo> {
        PokemonRepoImpl(get())
    }
    single { ThemePreferences(get()) }

    single { PokemonSearchRepository(api = get()) }

    // Add a coroutine scope for the callback
    single { CoroutineScope(Dispatchers.IO + SupervisorJob()) }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Step 1: Create new teams table
            db.execSQL("""
            CREATE TABLE IF NOT EXISTS teams (
                teamId TEXT PRIMARY KEY NOT NULL,
                teamName TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                isDefault INTEGER NOT NULL DEFAULT 0
            )
        """)

            // Step 2: Create default team
            val defaultTeamId = UUID.randomUUID().toString()
            db.execSQL("""
            INSERT INTO teams (teamId, teamName, createdAt, isDefault)
            VALUES ('$defaultTeamId', 'My Team', ${System.currentTimeMillis()}, 1)
        """)

            // Step 3: Create temporary table for new team_members structure
            db.execSQL("""
            CREATE TABLE IF NOT EXISTS team_members_new (
                id TEXT PRIMARY KEY NOT NULL,
                teamId TEXT NOT NULL,
                name TEXT NOT NULL,
                imageUrl TEXT NOT NULL,
                addedAt INTEGER NOT NULL,
                FOREIGN KEY(teamId) REFERENCES teams(teamId) ON DELETE CASCADE
            )
        """)

            // Step 4: Copy existing data to new table, assigning to default team
            db.execSQL("""
            INSERT INTO team_members_new (id, teamId, name, imageUrl, addedAt)
            SELECT 
                LOWER(HEX(RANDOMBLOB(16))),
                '$defaultTeamId',
                name,
                imageUrl,
                ${System.currentTimeMillis()}
            FROM team_members
        """)

            // Step 5: Drop old table
            db.execSQL("DROP TABLE team_members")

            // Step 6: Rename new table
            db.execSQL("ALTER TABLE team_members_new RENAME TO team_members")

            // Step 7: Create index
            db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_team_members_teamId 
            ON team_members(teamId)
        """)
        }
    }

    single {
        Room.databaseBuilder(
            get(),
            TeamDatabase::class.java,
            "pokeverseTeam_db"
        )
            .addMigrations(MIGRATION_2_3)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Create default team on first install
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
            .fallbackToDestructiveMigration()
            .build()
    }

    single { get<TeamDatabase>().teamDao() }

    single { get<TeamDatabase>().favoritesDao()}

    single { TeamMapper }

    // ViewModels
    viewModel {
        PokemonViewModel(get(), get(), get(), get(), get(), get())
    }

    viewModel {
        SettingsViewModel(androidContext())
    }

    viewModel { MatchViewModel(get(), get(), get()) }

    viewModel { QuizViewModel(get(), get()) }

    single { get<TeamDatabase>().gameScoreDao() }

    single { DescriptionRepo(androidContext()) }

    single {
        Room.databaseBuilder(
            get(),
            PokemonDatabase::class.java,
            "pokemon_db"
        ).build()
    }

    single { get<PokemonDatabase>().pokemonDao() }

    single { Gson() }

    single { SoundManager(get()) }
    viewModel { PokeGuessViewModel(repository = get(), billingManager = get()) }

}
