package com.example.alleyglow.di

import com.example.alleyglow.data.repository.AdSlotRepositoryImpl
import com.example.alleyglow.data.repository.AuthRepositoryImpl
import com.example.alleyglow.data.repository.CheckinRepositoryImpl
import com.example.alleyglow.data.repository.LocationRepositoryImpl
import com.example.alleyglow.data.repository.ShopReportRepositoryImpl
import com.example.alleyglow.domain.repository.ShopReportRepository
import com.example.alleyglow.domain.repository.CheckinRepository
import com.example.alleyglow.domain.repository.LocationRepository
import com.example.alleyglow.data.repository.ShiftRepositoryImpl
import com.example.alleyglow.domain.repository.ShiftRepository
import com.example.alleyglow.domain.repository.AuthRepository
import com.example.alleyglow.data.repository.ZoneRepositoryImpl
import com.example.alleyglow.data.tracking.TrackingRepositoryImpl
import com.example.alleyglow.domain.repository.TrackingRepository
import com.example.alleyglow.domain.repository.AdSlotRepository
import com.example.alleyglow.domain.repository.ZoneRepository
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

    @Binds
    @Singleton
    abstract fun bindCheckinRepository(impl: CheckinRepositoryImpl): CheckinRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds
    @Singleton
    abstract fun bindShopReportRepository(impl: ShopReportRepositoryImpl): ShopReportRepository

    @Binds
    @Singleton
    abstract fun bindTrackingRepository(impl: TrackingRepositoryImpl): TrackingRepository
}
