package uk.tw.energy.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.service.AccountService;
import uk.tw.energy.service.MeterReadingService;
import uk.tw.energy.service.PricePlanService;

@RestController
@RequestMapping("/price-plans")
public class PricePlanComparatorController {

    public static final String PRICE_PLAN_ID_KEY = "pricePlanId";
    public static final String PRICE_PLAN_COMPARISONS_KEY = "pricePlanComparisons";
    public static final int DAYS_WEEK = 7;
    /**
     * price per KWh
     */
    public static final double PRICE_UNIT = 0.2;
    private final PricePlanService pricePlanService;
    private final AccountService accountService;
    private final MeterReadingService meterReadingService;

    public PricePlanComparatorController(PricePlanService pricePlanService, AccountService accountService, MeterReadingService meterReadingService) {
        this.pricePlanService = pricePlanService;
        this.accountService = accountService;
        this.meterReadingService = meterReadingService;
    }



    @GetMapping("/compare-all/{smartMeterId}")
    public ResponseEntity<Map<String, Object>> calculatedCostForEachPricePlan(@PathVariable String smartMeterId) {
        String pricePlanId = accountService.getPricePlanIdForSmartMeterId(smartMeterId);
        Optional<Map<String, BigDecimal>> consumptionsForPricePlans =
                pricePlanService.getConsumptionCostOfElectricityReadingsForEachPricePlan(smartMeterId);

        if (!consumptionsForPricePlans.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> pricePlanComparisons = new HashMap<>();
        pricePlanComparisons.put(PRICE_PLAN_ID_KEY, pricePlanId);
        pricePlanComparisons.put(PRICE_PLAN_COMPARISONS_KEY, consumptionsForPricePlans.get());

        return consumptionsForPricePlans.isPresent()
                ? ResponseEntity.ok(pricePlanComparisons)
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/recommend/{smartMeterId}")
    public ResponseEntity<List<Map.Entry<String, BigDecimal>>> recommendCheapestPricePlans(
            @PathVariable String smartMeterId, @RequestParam(value = "limit", required = false) Integer limit) {
        Optional<Map<String, BigDecimal>> consumptionsForPricePlans =
                pricePlanService.getConsumptionCostOfElectricityReadingsForEachPricePlan(smartMeterId);

        if (!consumptionsForPricePlans.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        List<Map.Entry<String, BigDecimal>> recommendations =
                new ArrayList<>(consumptionsForPricePlans.get().entrySet());
        recommendations.sort(Comparator.comparing(Map.Entry::getValue));

        if (limit != null && limit < recommendations.size()) {
            recommendations = recommendations.subList(0, limit);
        }

        return ResponseEntity.ok(recommendations);
    }


    public Comparable<BigDecimal> calculateCostLastWeekByMeterId(String smartMeterId) {
        Optional<List<ElectricityReading>> readings = meterReadingService.getReadings(smartMeterId);
        if (readings.isEmpty() || readings.get().size() < 2) {
            throw new IllegalArgumentException("没有足够的数据进行计算");
        }

        List<ElectricityReading> erListLastWeek = readings.get().stream().filter(e ->
                e.time().isAfter(Instant.now().minus(DAYS_WEEK, ChronoUnit.DAYS)) &&
                        e.time().isBefore(Instant.now())).toList();
        ElectricityReading lastWeekEndReading = erListLastWeek.getFirst();
        ElectricityReading lastWeekStartReading = erListLastWeek.getLast();

        long hoursDifference = ChronoUnit.HOURS.between(lastWeekStartReading.time(), lastWeekEndReading.time());
        BigDecimal powerAvg = (lastWeekEndReading.reading().add(lastWeekStartReading.reading())).divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP);

        return powerAvg.multiply(BigDecimal.valueOf(hoursDifference)).multiply(BigDecimal.valueOf(PRICE_UNIT));
    }
}
