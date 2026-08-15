package com.dianping.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class LuaScriptConfig {

    @Bean
    public DefaultRedisScript<Long> seckillScript() throws IOException {
        return loadScript("seckill.lua");
    }

    @Bean
    public DefaultRedisScript<Long> unlockScript() throws IOException {
        return loadScript("unlock.lua");
    }

    private DefaultRedisScript<Long> loadScript(String path) throws IOException {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(text);
            script.setResultType(Long.class);
            return script;
        }
    }
}
