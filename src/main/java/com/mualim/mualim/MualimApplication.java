package com.mualim.mualim;

import com.mualim.mualim.service.Color;
import com.mualim.mualim.service.impl.colorImpl;
import lombok.extern.java.Log;
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
        final Color color = new colorImpl();
        log.info(color.print());
    }

}
