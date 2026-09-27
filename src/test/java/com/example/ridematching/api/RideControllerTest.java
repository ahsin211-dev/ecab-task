package com.example.ridematching.api;

import com.example.ridematching.api.impl.RideControllerImpl;
import com.example.ridematching.domain.DriverState;
import com.example.ridematching.domain.DriverStatus;
import com.example.ridematching.domain.Location;
import com.example.ridematching.domain.Ride;
import com.example.ridematching.exception.NoDriverAvailableException;
import com.example.ridematching.exception.RideAccessDeniedException;
import com.example.ridematching.exception.RideAlreadyCompletedException;
import com.example.ridematching.exception.RideNotFoundException;
import com.example.ridematching.exception.RiderHasActiveRideException;
import com.example.ridematching.service.RideDetails;
import com.example.ridematching.service.RideService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RideControllerImpl.class)
@Import(FixedClockConfig.class)
class RideControllerTest {

    private static final Instant REQUESTED_AT = Instant.parse("2026-01-01T08:00:00Z");
    private static final Instant COMPLETED_AT = Instant.parse("2026-01-01T08:30:00Z");
    private static final Location PICKUP = new Location(0, 0);
    private static final String VALID_RIDE_REQUEST = """
            {"riderId": "r1", "pickup": {"x": 0, "y": 0}}""";
    private static final String VALID_COMPLETION = """
            {"riderId": "r1"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RideService rideService;

    @Nested
    class RequestRide {

        @Test
        void returnsCreatedWithLinkToTheNewRide() throws Exception {
            when(rideService.requestRide("r1", PICKUP)).thenReturn(inProgressRide());

            postRide(VALID_RIDE_REQUEST)
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/v1/rides/ride-1"))
                    .andExpect(jsonPath("$.rideId").value("ride-1"))
                    .andExpect(jsonPath("$.riderId").value("r1"))
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.driver.driverId").value("d1"))
                    .andExpect(jsonPath("$.requestedAt").value(REQUESTED_AT.toString()))
                    .andExpect(jsonPath("$.completedAt").doesNotExist());
        }

        @Test
        void missingPickupIsBadRequest() throws Exception {
            postRide("""
                    {"riderId": "r1"}""")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.message").value("pickup: must not be null"));

            verifyNoInteractions(rideService);
        }

        @Test
        void noDriverAvailableIsConflict() throws Exception {
            when(rideService.requestRide("r1", PICKUP)).thenThrow(new NoDriverAvailableException());

            postRide(VALID_RIDE_REQUEST)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("NO_DRIVER_AVAILABLE"));
        }

        @Test
        void riderAlreadyOnARideIsConflict() throws Exception {
            when(rideService.requestRide("r1", PICKUP)).thenThrow(new RiderHasActiveRideException("r1"));

            postRide(VALID_RIDE_REQUEST)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("RIDER_HAS_ACTIVE_RIDE"));
        }
    }

    @Nested
    class CompleteRide {

        @Test
        void returnsCompletedRideWithFreedDriver() throws Exception {
            when(rideService.completeRide("ride-1", "r1")).thenReturn(completedRide());

            postCompletion("ride-1", VALID_COMPLETION)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.driver.status").value("AVAILABLE"))
                    .andExpect(jsonPath("$.completedAt").value(COMPLETED_AT.toString()));
        }

        @Test
        void differentRiderIsForbidden() throws Exception {
            when(rideService.completeRide("ride-1", "r1")).thenThrow(new RideAccessDeniedException("ride-1", "r1"));

            postCompletion("ride-1", VALID_COMPLETION)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("RIDE_ACCESS_DENIED"));
        }

        @Test
        void alreadyCompletedIsConflict() throws Exception {
            when(rideService.completeRide("ride-1", "r1")).thenThrow(new RideAlreadyCompletedException("ride-1"));

            postCompletion("ride-1", VALID_COMPLETION)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("RIDE_ALREADY_COMPLETED"));
        }
    }

    @Nested
    class GetRide {

        @Test
        void returnsTheRide() throws Exception {
            when(rideService.getRide("ride-1")).thenReturn(inProgressRide());

            mockMvc.perform(get("/api/v1/rides/ride-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rideId").value("ride-1"));
        }
    }

    private ResultActions postRide(String json) throws Exception {
        return mockMvc.perform(post("/api/v1/rides")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions postCompletion(String rideId, String json) throws Exception {
        return mockMvc.perform(post("/api/v1/rides/{rideId}/complete", rideId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private static RideDetails inProgressRide() {
        return new RideDetails(newRide(), new DriverState(new Location(3, 4), DriverStatus.ON_RIDE));
    }

    private static RideDetails completedRide() {
        Ride ride = newRide();
        ride.markCompleted(COMPLETED_AT);
        return new RideDetails(ride, new DriverState(new Location(3, 4), DriverStatus.AVAILABLE));
    }

    private static Ride newRide() {
        return Ride.start("ride-1", "r1", "d1", PICKUP, REQUESTED_AT);
    }
}
