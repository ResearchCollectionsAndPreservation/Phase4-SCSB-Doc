package org.recap.matchingalgorithm;

import org.recap.RecapConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.recap.RecapConstants.MATCHING_COUNTER_OPEN;
import static org.recap.RecapConstants.MATCHING_COUNTER_SHARED;
import static org.recap.RecapConstants.MATCHING_COUNTER_UPDATED_OPEN;
import static org.recap.RecapConstants.MATCHING_COUNTER_UPDATED_SHARED;

public class OngoingMatchingCounter {

    static List<String> scsbInstitutions;

    public OngoingMatchingCounter(List<String> institutions){
        scsbInstitutions=institutions;
    }

    private static Map<String, Map<String, Integer>> institutionCounterMap=new HashMap<>();

    public static synchronized void setSpecificInstitutionCounterMap(String institution,Map<String, Integer> counterMap){
        institutionCounterMap.put(institution,counterMap);
    }

    public static synchronized Map<String, Map<String, Integer>> getAllInstitutionCounterMap(){
        return institutionCounterMap;
    }

    public static synchronized void setAllInstitutionCounterMap(Map<String, Map<String, Integer>> institutionCounterMap){
         OngoingMatchingCounter.institutionCounterMap=institutionCounterMap;
    }

    public static synchronized Map<String, Integer> getSpecificInstitutionCounterMap(String institution){
        return institutionCounterMap.get(institution);
    }

    public static void reset(){
        for (String institution : scsbInstitutions) {
            Map<String,Integer> cgdCounterMap=new HashMap<>();
            cgdCounterMap.put(RecapConstants.MATCHING_COUNTER_SHARED,0);
            cgdCounterMap.put(RecapConstants.MATCHING_COUNTER_OPEN,0);
            cgdCounterMap.put(RecapConstants.MATCHING_COUNTER_UPDATED_SHARED,0);
            cgdCounterMap.put(RecapConstants.MATCHING_COUNTER_UPDATED_OPEN,0);
            institutionCounterMap.put(institution,cgdCounterMap);
        }

    }

    public static synchronized Map<String, Integer> updateCGDCounter(String institutionToUpdate,boolean isOpen){
        Map<String, Integer> institutionCgdCounter = institutionCounterMap.get(institutionToUpdate);
        if(isOpen){
            institutionCgdCounter.put(MATCHING_COUNTER_SHARED,institutionCgdCounter.get(MATCHING_COUNTER_SHARED)-1);
            institutionCgdCounter.put(MATCHING_COUNTER_OPEN,institutionCgdCounter.get(MATCHING_COUNTER_OPEN)+1);
            institutionCgdCounter.put(MATCHING_COUNTER_UPDATED_OPEN,institutionCgdCounter.get(MATCHING_COUNTER_UPDATED_OPEN)+1);
        }
        else {
            institutionCgdCounter.put(MATCHING_COUNTER_UPDATED_SHARED,institutionCgdCounter.get(MATCHING_COUNTER_UPDATED_SHARED)+1);
        }
        return institutionCgdCounter;
    }
}
