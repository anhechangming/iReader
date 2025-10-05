package com.library.ireaderbackend;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//@SpringBootApplication
@SpringBootApplication //排除数据源自动配置 数据库
@MapperScan("com.library.ireaderbackend.mapper")
public class IreaderBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(IreaderBackendApplication.class, args);
    }

}
