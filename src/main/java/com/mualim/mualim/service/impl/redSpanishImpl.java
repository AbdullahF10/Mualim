package com.mualim.mualim.service.impl;

import org.springframework.stereotype.Service;
import com.mualim.mualim.service.Red;

@Service
public class redSpanishImpl implements Red {

    @Override
    public String print() {
        return "Rojo";
    }
    
}