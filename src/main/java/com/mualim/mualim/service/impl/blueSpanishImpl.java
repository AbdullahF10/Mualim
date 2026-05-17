package com.mualim.mualim.service.impl;

import org.springframework.stereotype.Service;
import com.mualim.mualim.service.Blue;

@Service 
public class blueSpanishImpl implements Blue {

     @Override
    public String print() {
        return "Azul";
    }
    
}
