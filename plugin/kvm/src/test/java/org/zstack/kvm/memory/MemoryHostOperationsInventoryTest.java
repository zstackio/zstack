package org.zstack.kvm.memory;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MemoryHostOperationsInventoryTest {
    @Test public void projectsKnownFieldsAndPreservesUnknownStatusAndNulls() {
        Map<String,Object> row=new LinkedHashMap<>(); row.put("operation_id","0123456789abcdef0123456789abcdef"); row.put("vm_uuid","vm");
        row.put("kind","reclaim"); row.put("status","future_status"); row.put("started_at","2026-10-10T00:00:00Z");
        row.put("internal_path","must-not-leak");
        Map<String,Object> budget=new LinkedHashMap<>(); budget.put("actual_allocated_bytes",9007199254740993L); budget.put("reserved_bytes",4L); budget.put("budget_bytes",100L); budget.put("records",2); budget.put("protected_records",1);
        Map<String,Object> raw=new LinkedHashMap<>(); raw.put("entries",Collections.singletonList(row)); raw.put("total",1); raw.put("nextPage",null);
        raw.put("cq",new LinkedHashMap<String,Object>(){{put("C",2);put("Q",1);}}); raw.put("record_budget",budget); raw.put("internal","must-not-leak");
        MemoryHostOperationsInventory out=MemoryHostOperationsInventory.fromAgentState("host","host",raw);
        assertEquals("future_status",out.getEntries().get(0).getStatus()); assertNull(out.getEntries().get(0).getTimeout_at());
        assertEquals(Long.valueOf(9007199254740993L),out.getRecord_budget().getActual_allocated_bytes());
        assertNull(out.getNextPage()); assertEquals(Integer.valueOf(2),out.getCq().getC());
    }
    @Test public void acceptsWritebackRowsWithoutVmAndRejectsRoundedJsonNumbers() {
        Map<String,Object> row=new LinkedHashMap<>(); row.put("operation_id","0123456789abcdef0123456789abcdef"); row.put("vm_uuid","");
        row.put("kind","writeback"); row.put("status","running"); row.put("started_at","2026-10-10T00:00:00Z");
        Map<String,Object> budget=new LinkedHashMap<>(); budget.put("actual_allocated_bytes",9007199254740992d); budget.put("reserved_bytes",0L); budget.put("budget_bytes",1L); budget.put("records",1); budget.put("protected_records",0);
        Map<String,Object> raw=validRaw(row,budget);
        assertUnavailable(()->MemoryHostOperationsInventory.fromAgentState("host","host",raw));
        budget.put("actual_allocated_bytes",9007199254740993L);
        raw.remove("record_budget");
        MemoryHostOperationsInventory withoutOptionalBudget=MemoryHostOperationsInventory.fromAgentState("host","host",raw);
        assertEquals("",withoutOptionalBudget.getEntries().get(0).getVm_uuid()); assertNull(withoutOptionalBudget.getRecord_budget());
        budget.put("actual_allocated_bytes",Double.NaN);
        raw.put("record_budget",budget);
        assertUnavailable(()->MemoryHostOperationsInventory.fromAgentState("host","host",raw));
    }
    @Test public void rejectsMalformedOrWrongHostRatherThanSynthesizingZeros() {
        Map<String,Object> raw=new LinkedHashMap<>(); raw.put("entries",Collections.emptyList());
        assertUnavailable(()->MemoryHostOperationsInventory.fromAgentState("host","different",raw));
        raw.put("cq",Collections.emptyMap()); raw.put("record_budget",Collections.emptyMap()); raw.put("total",0); raw.put("nextPage",null);
        Map<String,Object> bad=new LinkedHashMap<>(raw); bad.put("entries",Collections.singletonList("bad"));
        assertUnavailable(()->MemoryHostOperationsInventory.fromAgentState("host","host",bad));
        Map<String,Object> b=new LinkedHashMap<>(); b.put("actual_allocated_bytes",1.5); b.put("reserved_bytes",0); b.put("budget_bytes",1); b.put("records",0); b.put("protected_records",0);
        Map<String,Object> badBudget=new LinkedHashMap<>(raw); badBudget.put("record_budget",b);
        assertUnavailable(()->MemoryHostOperationsInventory.fromAgentState("host","host",badBudget));
    }
    private static void assertUnavailable(Runnable call){try{call.run();fail("expected rejection");}catch(MemoryOperationException expected){assertEquals("MEMORY_OPERATION_QUERY_UNAVAILABLE",expected.getCode());}}
    private static Map<String,Object> validRaw(Map<String,Object> row,Map<String,Object> budget){
        Map<String,Object> raw=new LinkedHashMap<>(); raw.put("entries",Collections.singletonList(row)); raw.put("total",1); raw.put("nextPage",null);
        raw.put("cq",new LinkedHashMap<String,Object>(){{put("C",0);put("Q",0);}}); raw.put("record_budget",budget); return raw;
    }
}
