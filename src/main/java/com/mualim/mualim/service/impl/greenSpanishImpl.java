package com.mualim.mualim.service.impl;

import org.springframework.stereotype.Service;
import com.mualim.mualim.service.Green;

@Service 
public class greenSpanishImpl implements Green {

    @Override
    public String print() {
        return "Verde";
    }
    
}
