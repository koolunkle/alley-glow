package io.github.koolunkle.alleyglow.domain.repository

import io.github.koolunkle.alleyglow.domain.model.AdSlot

interface AdSlotRepository {
    suspend fun getAdSlots(): Result<List<AdSlot>>
}
