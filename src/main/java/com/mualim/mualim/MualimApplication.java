package com.mualim.mualim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;

@SpringBootApplication
@Log
public class MualimApplication implements CommandLineRunner{

    public static void main(String[] args) {
        SpringApplication.run(MualimApplication.class, args);
    }

    @Override
    public void run(final String... args){
        final Color color = new ColorImpl();
        log.info(color.print());
    }

}
