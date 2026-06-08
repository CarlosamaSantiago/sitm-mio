package edu.icesi.sitmmio.batchmaster.adapter;

import edu.icesi.sitmmio.batchmaster.service.Scheduler;
import edu.icesi.sitmmio.batchmaster.service.WorkerRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchMasterRangeTest {

    @Test
    void testRunRangePilot4() {
        // Setup
        WorkerRegistry registry = mock(WorkerRegistry.class);
        Scheduler scheduler = mock(Scheduler.class);
        Set<Integer> lines = Set.of(131);
        
        // Spy on the servant to count runMonth calls
        BatchMasterI servant = Mockito.spy(new BatchMasterI(registry, scheduler, lines));
        
        // Mock runMonth to avoid actual execution
        doReturn(new SITM.SpeedReport[0]).when(servant).runMonth(anyInt(), anyInt(), any());

        // Execute Pilot4 range: 2018-05 to 2019-05 (13 months)
        servant.runRange(2018, 5, 2019, 5, null);

        // Verify runMonth was called exactly 13 times
        verify(servant, times(13)).runMonth(anyInt(), anyInt(), any());
        
        // Verify specific month boundaries
        verify(servant).runMonth(eq(2018), eq(5), any());
        verify(servant).runMonth(eq(2018), eq(12), any());
        verify(servant).runMonth(eq(2019), eq(1), any());
        verify(servant).runMonth(eq(2019), eq(5), any());
    }

    @Test
    void testSingleMonthRange() {
        WorkerRegistry registry = mock(WorkerRegistry.class);
        Scheduler scheduler = mock(Scheduler.class);
        BatchMasterI servant = Mockito.spy(new BatchMasterI(registry, scheduler, Set.of(131)));
        doReturn(new SITM.SpeedReport[0]).when(servant).runMonth(anyInt(), anyInt(), any());

        servant.runRange(2019, 5, 2019, 5, null);
        
        verify(servant, times(1)).runMonth(2019, 5, null);
    }
}
