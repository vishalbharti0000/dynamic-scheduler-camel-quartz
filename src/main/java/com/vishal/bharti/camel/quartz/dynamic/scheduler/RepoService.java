package com.vishal.bharti.camel.quartz.dynamic.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RepoService {
    static int i = 0;

    public void test() {
        log.info("repo task executing: job" + i++ );
    }

}
