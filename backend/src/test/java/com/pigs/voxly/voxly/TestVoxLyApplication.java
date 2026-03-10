package com.pigs.voxly.voxly;

import com.pigs.voxly.VoxLyApplication;
import org.springframework.boot.SpringApplication;

public class TestVoxLyApplication {

    public static void main(String[] args) {
        SpringApplication.from(VoxLyApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
