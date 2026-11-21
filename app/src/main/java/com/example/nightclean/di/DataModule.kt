package com.example.nightclean.di

import com.example.nightclean.data.repository.AdSlotRepositoryImpl
import com.example.nightclean.data.repository.AuthRepositoryImpl
import com.example.nightclean.data.repository.ShiftRepositoryImpl
import com.example.nightclean.domain.repository.ShiftRepository
import com.example.nightclean.domain.repository.AuthRepository
import com.example.nightclean.data.repository.ZoneRepositoryImpl
import com.example.nightclean.domain.repository.AdSlotRepository
import com.example.nightclean.domain.repository.ZoneRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindZoneRepository(impl: ZoneRepositoryImpl): ZoneRepository

    @Binds
    @Singleton
    abstract fun bindAdSlotRepository(impl: AdSlotRepositoryImpl): AdSlotRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindShiftRepository(impl: ShiftRepositoryImpl): ShiftRepository
}
