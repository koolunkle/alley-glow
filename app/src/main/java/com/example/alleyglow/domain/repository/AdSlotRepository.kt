package com.example.alleyglow.domain.repository

import com.example.alleyglow.domain.model.AdSlot

interface AdSlotRepository {
    suspend fun getAdSlots(): Result<List<AdSlot>>
}
