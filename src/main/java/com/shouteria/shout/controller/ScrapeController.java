package com.shouteria.shout.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.shouteria.shout.service.ScrapeService;

import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/scrape")
public class ScrapeController {
  private static final Logger LOGGER = Logger.getLogger(ScrapeController.class.getName());

  @Autowired
  private ScrapeService scrapeService;

  @GetMapping()
  public ResponseEntity<List<String>> scrapeURL(@RequestParam String uri) {
    LOGGER.info("Starting to scrape page : " + uri);

    List<String> list = null;
    try {
      list = scrapeService.scrape(uri);
      LOGGER.info("Page scraped in " + Thread.currentThread() + " thread : " + uri);
    } catch (Exception e) {
      LOGGER.warning("Page could not scraped : " + e.getMessage());
    }

    assert list != null;
    return ResponseEntity.ok(list);
  }
}
