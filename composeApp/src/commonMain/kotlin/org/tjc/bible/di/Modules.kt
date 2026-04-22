package org.tjc.bible.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.module
import org.tjc.bible.cache.BibleDatabase
import org.tjc.bible.data.abs.AbsBibleRepository
import org.tjc.bible.data.bss.BssBibleRepository
import org.tjc.bible.data.cache.DriverFactory
import org.tjc.bible.data.local.PreferenceStorage
import org.tjc.bible.data.repository.CachedBibleRepository
import org.tjc.bible.data.repository.CompositeBibleRepository
import org.tjc.bible.domain.repository.BibleRepository
import org.tjc.bible.domain.usecase.GetBibleVersionsUseCase
import org.tjc.bible.domain.usecase.GetVersesUseCase
import org.tjc.bible.domain.usecase.SearchUseCase
import org.tjc.bible.presentation.bible.BibleViewModel

expect val platformModule: Module

val appModule = module {
    includes(platformModule)
    
    single {
        val driver = get<DriverFactory>().createDriver()
        BibleDatabase(driver)
    }

    single {
        HttpClient {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        prettyPrint = true
                        isLenient = true
                    }
                )
            }
            install(HttpCache)
        }
    }

    singleOf(::PreferenceStorage)
    
    single { AbsBibleRepository(get()) }
    single { BssBibleRepository(get()) }
    single { 
        CompositeBibleRepository(
            repositories = listOf(
                get<AbsBibleRepository>(),
                get<BssBibleRepository>()
            )
        ) 
    }
    single { CachedBibleRepository(get<CompositeBibleRepository>(), get()) } bind BibleRepository::class
    
    factoryOf(::GetBibleVersionsUseCase)
    factoryOf(::GetVersesUseCase)
    factoryOf(::SearchUseCase)

    singleOf(::BibleViewModel)
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(appModule)
    }
