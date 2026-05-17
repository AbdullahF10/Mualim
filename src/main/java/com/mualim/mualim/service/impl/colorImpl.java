package com.mualim.mualim.service.impl;

import com.mualim.mualim.service.Blue;
import com.mualim.mualim.service.Color;
import com.mualim.mualim.service.Green;
import com.mualim.mualim.service.Red;
import org.springframework.stereotype.Component;;

@Component
public class colorImpl implements Color {

    private Red red;
    private Green green;
    private Blue blue;


    public colorImpl(Red red, Green green, Blue blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
       
    }

    @Override
    public String print() {
        return String.join(", ", red.print(), blue.print(), green.print());
    }
}
