package com.jasim.kcoinapi.repository

import com.jasim.kcoinapi.event.entity.EventEntity
import com.jasim.kcoinapi.event.repository.EventRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.time.temporal.ChronoUnit

@ActiveProfiles("test")
@DataJpaTest
class EventRepositoryTest {
    @Autowired
    lateinit var eventRepository: EventRepository
    lateinit var baseEvent: EventEntity
    lateinit var name: String
    lateinit var starts: Instant
    lateinit var ends: Instant

    @BeforeEach
    fun setUp() {
        name = "2025 여름휴가 이벤트"
        starts = Instant.now().minus(1, ChronoUnit.DAYS)
        ends = Instant.now().plus(7, ChronoUnit.DAYS)
        baseEvent = EventEntity(
            pEventName = name,
            pStartsAt = starts,
            pEndsAt = ends
        )
    }

    @Test
    @DisplayName("저장 및 조회 기본 필드와 Audit 검증")
    fun `저장_조회_테스트`() {
        // given
        val before = Instant.now()

        // when
        val saved = eventRepository.saveAndFlush(baseEvent)
        val find = eventRepository.findById(saved.id!!).get()

        // then
        assertThat(find.id).isNotNull()
        assertThat(find.eventName).isEqualTo(name)
        assertThat(find.startsAt).isEqualTo(starts)
        assertThat(find.endsAt).isEqualTo(ends)

        // then
        assertThat(find.rewards)
            .`as`("저장 시 보상 리스트는 빈 리스트여야 한다")
            .isEmpty()
        assertThat(find.coins)
            .`as`("저장 시 코인 리스트는 빈 리스트여야 한다")
            .isEmpty()

        // then
        assertThat(find.audit.createdAt)
            .isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS))
        assertThat(find.audit.updatedAt)
            .isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS))
    }
}