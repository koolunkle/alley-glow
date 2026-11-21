package com.example.nightclean.domain.repository

import com.example.nightclean.domain.model.AdSlot

interface AdSlotRepository {
    suspend fun getAdSlots(): Result<List<AdSlot>>
}
