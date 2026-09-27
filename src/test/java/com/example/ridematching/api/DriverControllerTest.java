package com.example.ridematching.api;

import com.example.ridematching.api.impl.DriverControllerImpl;
import com.example.ridematching.domain.DriverState;
import com.example.ridematching.domain.DriverStatus;
import com.example.ridematching.domain.Location;
import com.example.ridematching.exception.DriverNotFoundException;
import com.example.ridematching.exception.DriverOnRideException;
import com.example.ridematching.matching.RankedDriver;
import com.example.ridematching.service.DriverDetails;
import com.example.ridematching.service.DriverService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DriverControllerImpl.class)
@Import(FixedClockConfig.class)
class DriverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DriverService driverService;

    @Nested
    class ListDrivers {

        @Test
        void returnsEveryDriverWithStatus() throws Exception {
            when(driverService.findAll()).thenReturn(List.of(
                    new DriverDetails("d1", state(1, 2, DriverStatus.OFFLINE)),
                    new DriverDetails("d2", state(3, 4, DriverStatus.ON_RIDE))));

            mockMvc.perform(get("/api/v1/drivers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].driverId").value("d1"))
                    .andExpect(jsonPath("$[0].location.x").value(1.0))
                    .andExpect(jsonPath("$[0].status").value("OFFLINE"))
                    .andExpect(jsonPath("$[1].status").value("ON_RIDE"));
        }
    }

    @Nested
    class RegisterOrUpdateDriver {

        @Test
        void returnsDriverWithNewState() throws Exception {
            when(driverService.updateDriver("d1", new Location(1, 2), true))
                    .thenReturn(state(1, 2, DriverStatus.AVAILABLE));

            putDriver("d1", """
                    {"x": 1.0, "y": 2.0, "available": true}""")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.driverId").value("d1"))
                    .andExpect(jsonPath("$.location.x").value(1.0))
                    .andExpect(jsonPath("$.location.y").value(2.0))
                    .andExpect(jsonPath("$.status").value("AVAILABLE"));
        }

        @Test
        void missingAvailableIsBadRequest() throws Exception {
            putDriver("d1", """
                    {"x": 1.0, "y": 2.0}""")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.message").value(containsString("available")))
                    .andExpect(jsonPath("$.timestamp").value(FixedClockConfig.NOW.toString()));

            verifyNoInteractions(driverService);
        }

        @Test
        void driverOnRideIsConflict() throws Exception {
            when(driverService.updateDriver("d1", new Location(1, 2), false))
                    .thenThrow(new DriverOnRideException("d1"));

            putDriver("d1", """
                    {"x": 1.0, "y": 2.0, "available": false}""")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("DRIVER_ON_RIDE"));
        }
    }

    @Nested
    class UpdateLocation {

        @Test
        void returnsDriverAtNewLocation() throws Exception {
            when(driverService.updateLocation("d1", new Location(5, 6)))
                    .thenReturn(state(5, 6, DriverStatus.ON_RIDE));

            putLocation("d1", """
                    {"x": 5, "y": 6}""")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.location.x").value(5.0))
                    .andExpect(jsonPath("$.status").value("ON_RIDE"));
        }

        @Test
        void unknownDriverIsNotFound() throws Exception {
            when(driverService.updateLocation("ghost", new Location(1, 1)))
                    .thenThrow(new DriverNotFoundException("ghost"));

            putLocation("ghost", """
                    {"x": 1, "y": 1}""")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }

    @Nested
    class FindNearestDrivers {

        @Test
        void returnsDriversClosestFirstWithDistance() throws Exception {
            when(driverService.findNearestAvailable(new Location(0, 0), 2)).thenReturn(List.of(
                    new RankedDriver("near", state(3, 4, DriverStatus.AVAILABLE), 5.0),
                    new RankedDriver("far", state(6, 8, DriverStatus.AVAILABLE), 10.0)));

            nearest("x", "0", "y", "0", "limit", "2")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].driverId").value("near"))
                    .andExpect(jsonPath("$[0].distance").value(5.0))
                    .andExpect(jsonPath("$[1].driverId").value("far"));
        }

        @Test
        void limitAboveOneHundredIsBadRequest() throws Exception {
            nearest("x", "0", "y", "0", "limit", "101")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.message").value(containsString("limit")));

            verifyNoInteractions(driverService);
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        void unexpectedErrorHidesInternalDetails() throws Exception {
            when(driverService.findNearestAvailable(any(), anyInt()))
                    .thenThrow(new IllegalStateException("database password is hunter2"));

            nearest("x", "0", "y", "0")
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                    .andExpect(jsonPath("$.message").value("Something went wrong on our side"));
        }
    }

    private ResultActions putDriver(String driverId, String json) throws Exception {
        return mockMvc.perform(put("/api/v1/drivers/{id}", driverId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions putLocation(String driverId, String json) throws Exception {
        return mockMvc.perform(put("/api/v1/drivers/{id}/location", driverId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    /** Query params as name/value pairs, e.g. nearest("x", "0", "y", "0"). */
    private ResultActions nearest(String... nameValuePairs) throws Exception {
        var request = get("/api/v1/drivers/nearest");
        for (int i = 0; i < nameValuePairs.length; i += 2) {
            request.param(nameValuePairs[i], nameValuePairs[i + 1]);
        }
        return mockMvc.perform(request);
    }

    private static DriverState state(double x, double y, DriverStatus status) {
        return new DriverState(new Location(x, y), status);
    }
}
