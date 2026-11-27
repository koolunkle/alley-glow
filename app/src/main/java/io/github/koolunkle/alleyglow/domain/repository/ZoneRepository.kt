package io.github.koolunkle.alleyglow.domain.repository

import io.github.koolunkle.alleyglow.domain.model.Zone

interface ZoneRepository {
    suspend fun getZones(): Result<List<Zone>>
}
