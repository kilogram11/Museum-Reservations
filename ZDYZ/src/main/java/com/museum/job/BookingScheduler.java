package com.museum.job;

import com.museum.mapper.JoinMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 预约定时任务
 */
@Slf4j
@Component
public class BookingScheduler {

    @Autowired
    private JoinMapper joinMapper;

    @Autowired
    private com.museum.mapper.IdentityMapper identityMapper;

    /**
     * 自动检查逾期 & 黑名单管理
     * 每5分钟执行一次: 0 5***?
     */


    @PostConstruct
    @Scheduled(cron = "0 */5 * * * ?")
    public void autoCheck() {
        log.info("开始执行自动任务: 逾期检查 & 黑名单管理...");
        try {
            long now = System.currentTimeMillis();
            
            // 1. 逾期检查
            joinMapper.updateOverdueStatus(now);
            
            // 2. 更新违约次数
            identityMapper.updateBanStatistics();
            
            // 3. 触发拉黑 (封禁30天)
            long endTime = now + 30L * 24 * 60 * 60 * 1000;
            identityMapper.doBan(now, endTime);
            
            // 4. 自动解封 (启动时和每次循环都检查一下，防止过期未解封)
            identityMapper.autoUnban(now);

            log.info("自动任务执行完成");
        } catch (Exception e) {
            log.error("自动任务执行失败", e);
        }
    }
}
