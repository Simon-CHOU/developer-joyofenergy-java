package uk.tw.energy.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.tw.energy.domain.ElectricityReading;

@Service
public class MeterReadingService {

    // 存储智能电表ID和对应的用电量读数列表的映射
    private final Map<String, List<ElectricityReading>> meterAssociatedReadings;

    /**
     * 构造函数，初始化智能电表读数映射
     *
     * @param meterAssociatedReadings 智能电表ID和对应的用电量读数列表的映射
     */
    public MeterReadingService(Map<String, List<ElectricityReading>> meterAssociatedReadings) {
        this.meterAssociatedReadings = meterAssociatedReadings;
    }

    /**
     * 根据智能电表ID获取用电量读数列表
     *
     * @param smartMeterId 智能电表ID
     * @return 用电量读数列表，如果未找到则返回空的Optional
     */
    public Optional<List<ElectricityReading>> getReadings(String smartMeterId) {
        return Optional.ofNullable(meterAssociatedReadings.get(smartMeterId));
    }

    /**
     * 存储用电量读数到指定的智能电表ID
     *
     * @param smartMeterId 智能电表ID
     * @param electricityReadings 要存储的用电量读数列表
     */
    public void storeReadings(String smartMeterId, List<ElectricityReading> electricityReadings) {
        if (!meterAssociatedReadings.containsKey(smartMeterId)) {
            meterAssociatedReadings.put(smartMeterId, new ArrayList<>());
        }
        meterAssociatedReadings.get(smartMeterId).addAll(electricityReadings);
    }
}
