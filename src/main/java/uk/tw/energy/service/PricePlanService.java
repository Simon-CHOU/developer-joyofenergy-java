package uk.tw.energy.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.PricePlan;

@Service
public class PricePlanService {

    private final List<PricePlan> pricePlans;
    private final MeterReadingService meterReadingService;

    /**
     * 构造函数，初始化价格计划列表和电表读数服务
     *
     * @param pricePlans 价格计划列表
     * @param meterReadingService 电表读数服务
     */
    public PricePlanService(List<PricePlan> pricePlans, MeterReadingService meterReadingService) {
        this.pricePlans = pricePlans;
        this.meterReadingService = meterReadingService;
    }

    /**
     * 获取每个价格计划的用电量读数的消费成本
     *
     * @param smartMeterId 智能电表ID
     * @return 包含每个价格计划成本的映射，如果未找到读数则返回空的Optional
     */
    public Optional<Map<String, BigDecimal>> getConsumptionCostOfElectricityReadingsForEachPricePlan(
            String smartMeterId) {
        Optional<List<ElectricityReading>> electricityReadings = meterReadingService.getReadings(smartMeterId);

        if (!electricityReadings.isPresent()) {
            return Optional.empty();
        }

        return Optional.of(pricePlans.stream()
               .collect(Collectors.toMap(PricePlan::getPlanName, t -> calculateCost(electricityReadings.get(), t))));
    }

    /**
     * 计算给定价格计划的用电量读数的成本
     *
     * @param electricityReadings 用电量读数列表
     * @param pricePlan 价格计划
     * @return 成本
     */
    private BigDecimal calculateCost(List<ElectricityReading> electricityReadings, PricePlan pricePlan) {
        BigDecimal average = calculateAverageReading(electricityReadings);
        BigDecimal timeElapsed = calculateTimeElapsed(electricityReadings);

        BigDecimal averagedCost = average.divide(timeElapsed, RoundingMode.HALF_UP);
        return averagedCost.multiply(pricePlan.getUnitRate());
    }

    /**
     * 计算用电量读数的平均值
     *
     * @param electricityReadings 用电量读数列表
     * @return 平均值
     */
    private BigDecimal calculateAverageReading(List<ElectricityReading> electricityReadings) {
        BigDecimal summedReadings = electricityReadings.stream()
               .map(ElectricityReading::reading)
               .reduce(BigDecimal.ZERO, (reading, accumulator) -> reading.add(accumulator));

        return summedReadings.divide(BigDecimal.valueOf(electricityReadings.size()), RoundingMode.HALF_UP);
    }

    /**
     * 计算用电量读数的时间间隔
     *
     * @param electricityReadings 用电量读数列表
     * @return 时间间隔
     */
    private BigDecimal calculateTimeElapsed(List<ElectricityReading> electricityReadings) {
        ElectricityReading first = electricityReadings.stream()
                .min(Comparator.comparing(ElectricityReading::time))
                .get();

        ElectricityReading last = electricityReadings.stream()
                .max(Comparator.comparing(ElectricityReading::time))
                .get();

        return BigDecimal.valueOf(Duration.between(first.time(), last.time()).getSeconds() / 3600.0);
    }
}
