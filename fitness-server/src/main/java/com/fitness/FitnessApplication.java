package com.fitness;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.fitness.mapper")
@EnableScheduling
public class FitnessApplication {
    public static void main(String[] args) {
        SpringApplication.run(FitnessApplication.class, args);
        System.out.println("========================================");
        System.out.println("  健身推荐管理系统启动成功！");
        System.out.println("  http://localhost:8080");
        System.out.println("========================================");
    }
}
