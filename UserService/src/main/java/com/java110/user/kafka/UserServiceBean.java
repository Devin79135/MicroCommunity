package com.java110.user.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by wuxw on 2018/4/15.
 */
@Configuration
public class UserServiceBean {
    @Bean
    public UserServiceKafka listener() {
        return new UserServiceKafka();
    }

}
