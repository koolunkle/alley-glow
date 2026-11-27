package com.example.alleyglow.domain.repository

import com.example.alleyglow.domain.model.Zone

interface ZoneRepository {
    suspend fun getZones(): Result<List<Zone>>
}
