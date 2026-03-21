package com.luxferre.chroniqo.service.event;

import com.luxferre.chroniqo.model.User;
import com.vaadin.flow.shared.Registration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Unit tests for the {@link Broadcaster} base class via a minimal concrete subclass.
 */
@DisplayName("Broadcaster Tests")
class BroadcasterTest {

    /**
     * Minimal concrete subclass — exposes broadcast() for direct testing
     * without requiring a Spring context or @TransactionalEventListener wiring.
     */
    static class TestBroadcaster extends Broadcaster<EntityChangedEvent> {
        @Override
        public void handle(EntityChangedEvent event) {
            broadcast(event);
        }
    }

    private TestBroadcaster broadcaster;
    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        broadcaster = new TestBroadcaster();

        userA = new User();
        userA.setEmail("alice@example.com");
        userA.setFirstName("Alice");
        userA.setLastName("A");
        userA.setPasswordHash("hash");

        userB = new User();
        userB.setEmail("bob@example.com");
        userB.setFirstName("Bob");
        userB.setLastName("B");
        userB.setPasswordHash("hash");
    }

    // =========================================================================
    // Registration
    // =========================================================================

    @Nested
    @DisplayName("Registration")
    class RegistrationTest {

        @Test
        @DisplayName("register returns a non-null Registration")
        void register_returnsNonNullRegistration() {
            Registration reg = broadcaster.register(userA, event -> {
            });
            assertThat(reg).isNotNull();
        }

        @Test
        @DisplayName("Same user registered twice — both listeners receive the event")
        void register_sameUser_multipleTimes_allListenersReceiveEvent() throws InterruptedException {
            AtomicInteger callCount = new AtomicInteger();
            CountDownLatch latch = new CountDownLatch(2);

            broadcaster.register(userA, event -> {
                callCount.incrementAndGet();
                latch.countDown();
            });
            broadcaster.register(userA, event -> {
                callCount.incrementAndGet();
                latch.countDown();
            });

            broadcaster.handle(new EntityChangedEvent(userA, BroadcasterTest.class));

            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(callCount.get()).isEqualTo(2);
        }

        @Test
        @DisplayName("Different users do not receive each other's events")
        void register_differentUsers_doNotReceiveEachOthersEvents() throws InterruptedException {
            AtomicInteger userBCallCount = new AtomicInteger();
            CountDownLatch latch = new CountDownLatch(1);

            broadcaster.register(userA, event -> latch.countDown());
            broadcaster.register(userB, event -> userBCallCount.incrementAndGet());

            broadcaster.handle(new EntityChangedEvent(userA, BroadcasterTest.class));

            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
            Thread.sleep(100); // give userB listener time to fire if it incorrectly does
            assertThat(userBCallCount.get()).isZero();
        }
    }

    // =========================================================================
    // Deregistration
    // =========================================================================

    @Nested
    @DisplayName("Deregistration")
    class Deregistration {

        @Test
        @DisplayName("remove stops the listener from receiving further events")
        void remove_stopsListenerFromReceivingFurtherEvents() throws InterruptedException {
            AtomicInteger callCount = new AtomicInteger();
            CountDownLatch firstLatch = new CountDownLatch(1);

            Registration reg = broadcaster.register(userA, event -> {
                callCount.incrementAndGet();
                firstLatch.countDown();
            });

            broadcaster.handle(new EntityChangedEvent(userA, BroadcasterTest.class));
            assertThat(firstLatch.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(callCount.get()).isEqualTo(1);

            reg.remove();

            broadcaster.handle(new EntityChangedEvent(userA, BroadcasterTest.class));
            Thread.sleep(100);
            assertThat(callCount.get()).isEqualTo(1); // still 1 — not called again
        }

        @Test
        @DisplayName("remove called twice does not throw")
        void remove_calledTwice_doesNotThrow() {
            Registration reg = broadcaster.register(userA, event -> {
            });
            reg.remove();
            assertThatNoException().isThrownBy(reg::remove);
        }

        @Test
        @DisplayName("remove only removes the target listener — others still receive events")
        void remove_onlyRemovesTargetListener_othersStillActive() throws InterruptedException {
            AtomicInteger listenerACount = new AtomicInteger();
            AtomicInteger listenerBCount = new AtomicInteger();
            CountDownLatch latch = new CountDownLatch(1);

            Registration regA = broadcaster.register(userA, event -> listenerACount.incrementAndGet());
            broadcaster.register(userA, event -> {
                listenerBCount.incrementAndGet();
                latch.countDown();
            });

            regA.remove();

            broadcaster.handle(new EntityChangedEvent(userA, BroadcasterTest.class));
            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();

            Thread.sleep(100);
            assertThat(listenerACount.get()).isZero();
            assertThat(listenerBCount.get()).isEqualTo(1);
        }
    }

    // =========================================================================
    // Broadcast edge cases
    // =========================================================================

    @Nested
    @DisplayName("Broadcast edge cases")
    class BroadcastEdgeCases {

        @Test
        @DisplayName("broadcast with no listeners does not throw")
        void broadcast_withNoListeners_doesNotThrow() {
            assertThatNoException().isThrownBy(() ->
                    broadcaster.handle(new EntityChangedEvent(userA, BroadcasterTest.class)));
        }

        @Test
        @DisplayName("broadcast after all listeners removed does not throw")
        void broadcast_afterAllListenersRemoved_doesNotThrow() {
            Registration reg = broadcaster.register(userA, event -> {
            });
            reg.remove();

            assertThatNoException().isThrownBy(() ->
                    broadcaster.handle(new EntityChangedEvent(userA, BroadcasterTest.class)));
        }

        @Test
        @DisplayName("broadcast is strictly user-scoped — other user's listener not notified")
        void broadcast_isUserScoped_otherUserListenerNotNotified() throws InterruptedException {
            CountDownLatch userALatch = new CountDownLatch(1);
            AtomicInteger userBCallCount = new AtomicInteger();

            broadcaster.register(userA, event -> userALatch.countDown());
            broadcaster.register(userB, event -> userBCallCount.incrementAndGet());

            broadcaster.handle(new EntityChangedEvent(userA, BroadcasterTest.class));

            assertThat(userALatch.await(2, TimeUnit.SECONDS)).isTrue();
            Thread.sleep(100);
            assertThat(userBCallCount.get()).isZero();
        }

        @Test
        @DisplayName("broadcast executes listeners asynchronously on a different thread")
        void broadcast_executesListenerAsynchronously() throws InterruptedException {
            Thread callingThread = Thread.currentThread();
            AtomicInteger differentThreadCount = new AtomicInteger();
            CountDownLatch latch = new CountDownLatch(1);

            broadcaster.register(userA, event -> {
                if (Thread.currentThread() != callingThread) {
                    differentThreadCount.incrementAndGet();
                }
                latch.countDown();
            });

            broadcaster.handle(new EntityChangedEvent(userA, BroadcasterTest.class));

            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(differentThreadCount.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("broadcast delivers event to all registered listeners")
        void broadcast_multipleListeners_allReceiveEvent() throws InterruptedException {
            int listenerCount = 5;
            CountDownLatch latch = new CountDownLatch(listenerCount);

            for (int i = 0; i < listenerCount; i++) {
                broadcaster.register(userA, event -> latch.countDown());
            }

            broadcaster.handle(new EntityChangedEvent(userA, BroadcasterTest.class));

            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        }
    }

    // =========================================================================
    // Destroy
    // =========================================================================

    @Nested
    @DisplayName("Destroy")
    class Destroy {

        @Test
        @DisplayName("destroy does not throw")
        void destroy_doesNotThrow() {
            broadcaster.register(userA, event -> {
            });
            assertThatNoException().isThrownBy(() -> broadcaster.destroy());
        }
    }
}