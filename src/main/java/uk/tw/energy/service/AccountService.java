package uk.tw.energy.service;

import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    // 智能电表ID到价格计划ID的映射
    private final Map<String, String> smartMeterToPricePlanAccounts;

    /**
     * 构造函数，初始化智能电表到价格计划账户的映射
     *
     * @param smartMeterToPricePlanAccounts 智能电表ID到价格计划ID的映射
     */
    public AccountService(Map<String, String> smartMeterToPricePlanAccounts) {
        this.smartMeterToPricePlanAccounts = smartMeterToPricePlanAccounts;
    }

    /**
     * 根据智能电表ID获取价格计划ID
     *
     * @param smartMeterId 智能电表ID
     * @return 价格计划ID，如果未找到则返回null
     */
    public String getPricePlanIdForSmartMeterId(String smartMeterId) {
        return smartMeterToPricePlanAccounts.get(smartMeterId);
    }
}
