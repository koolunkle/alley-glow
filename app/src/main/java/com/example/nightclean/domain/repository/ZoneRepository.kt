package com.example.nightclean.domain.repository

import com.example.nightclean.domain.model.Zone

interface ZoneRepository {
    suspend fun getZones(): Result<List<Zone>>
}
