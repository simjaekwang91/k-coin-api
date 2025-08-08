package com.jasim.kcoinapi.event.repository

import com.jasim.kcoinapi.event.entity.EventEntity
import org.springframework.data.jpa.repository.JpaRepository

interface EventRepository: JpaRepository<EventEntity, Long> {
}