package io.github.koolunkle.alleyglow.di

import io.github.koolunkle.alleyglow.data.repository.AdSlotRepositoryImpl
import io.github.koolunkle.alleyglow.data.repository.AuthRepositoryImpl
import io.github.koolunkle.alleyglow.data.repository.CheckinRepositoryImpl
import io.github.koolunkle.alleyglow.data.repository.LocationRepositoryImpl
import io.github.koolunkle.alleyglow.data.repository.ShopReportRepositoryImpl
import io.github.koolunkle.alleyglow.domain.repository.ShopReportRepository
import io.github.koolunkle.alleyglow.domain.repository.CheckinRepository
import io.github.koolunkle.alleyglow.domain.repository.LocationRepository
import io.github.koolunkle.alleyglow.data.repository.ShiftRepositoryImpl
import io.github.koolunkle.alleyglow.domain.repository.ShiftRepository
import io.github.koolunkle.alleyglow.domain.repository.AuthRepository
import io.github.koolunkle.alleyglow.data.repository.ZoneRepositoryImpl
import io.github.koolunkle.alleyglow.data.tracking.TrackingRepositoryImpl
import io.github.koolunkle.alleyglow.domain.repository.TrackingRepository
import io.github.koolunkle.alleyglow.domain.repository.AdSlotRepository
import io.github.koolunkle.alleyglow.domain.repository.ZoneRepository
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
