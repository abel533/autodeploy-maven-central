package com.github.abel533.autodeploymavencentral;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class AutodeployMavenCentralApplication {

  public static void main(String[] args) {
    SpringApplication.run(AutodeployMavenCentralApplication.class, args);
  }

  @RestController
  class DemoController {

    @RequestMapping({"", "/", "/hello"})
    public String hello() {
      return "hello";
    }
  }

}
