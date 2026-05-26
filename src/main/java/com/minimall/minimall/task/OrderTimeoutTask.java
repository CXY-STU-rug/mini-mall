
package com.minimall.minimall.task;

import com.minimall.minimall.service.IOrdersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component                              // 让 Spring 扫描到这个类
public class OrderTimeoutTask {

    @Autowired
    private IOrdersService ordersService;

    @Scheduled(cron = "0 * * * * *")    // 每分钟整触发
    public void closeTimeoutOrders() {
        log.info("开始扫描超时订单...");
        int count = ordersService.closeTimeoutOrders();
        log.info("超时关单完成，本次关闭 {} 笔订单", count);
    }
}