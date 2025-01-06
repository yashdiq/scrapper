package com.shouteria.shout.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
public class ScrapeService {
  private static final Logger LOGGER = Logger.getLogger(ScrapeService.class.getName());

  public List<String> scrape(String uri) {
    ChromeOptions options = new ChromeOptions();
    // options.addArguments("--headless");
    // options.addArguments("--headless=new");
    options.addArguments("--disable-dev-shm-usage");
    options.addArguments("--no-sandbox");

    WebDriver driver = new ChromeDriver(options);
    driver.get(uri);

    List<String> paragraphs = new ArrayList<>();
    switch (uri) {
      case "https://www.londonstockexchange.com/privacy-and-cookie-policy" ->
              paragraphs = this.fromLondonExchange(driver);
      case "https://hamilton.govt.nz/privacy-policy" -> paragraphs = this.fromHamilton(driver);
      case "https://unsplash.com/privacy" -> paragraphs = this.fromUnsplash(driver);
      case "https://account.weverse.io/en/policies/cookie-policy" -> paragraphs = this.fromWeverse(driver);
      default -> {
        // default is set to https://www.serverless.com/legal/privacy
        paragraphs = this.fromServerless(driver);
      }
    }

    return paragraphs;
  }

  /**
   * Scrape from https://www.londonstockexchange.com/privacy-and-cookie-policy
   * @param  driver (WebDriver)
   * @return List<String> paragraphs
   */
  public List<String> fromLondonExchange(WebDriver driver) {
    List<String> paragraphs = new ArrayList<>();

    // Wait for the URL redirected to new URL
    Wait<WebDriver> wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    Boolean isChanged = wait.until(ExpectedConditions.urlContains("lseg.com"));

    if (isChanged) {
      String newUrl = driver.getCurrentUrl();

      driver.get(newUrl);

//      get root element
      By condition = By.cssSelector("#root > div > main > div.Section.Section--white > div.Section-inner");
      wait.until(ExpectedConditions.visibilityOfElementLocated(condition));
      WebElement main = driver.findElement(condition);

      List<WebElement> mainElements = main.findElements(By.cssSelector("div > div > div > div:nth-child(2) > div"));

      for (WebElement el : mainElements) {
        try {
          WebElement expanded = el.findElement(By.cssSelector(".ExpandableItem-content.is-collapsed"));
          if (expanded != null) {
            JavascriptExecutor executor = (JavascriptExecutor) driver;
            executor.executeScript("arguments[0].setAttribute('style','display:block;');", expanded);
            paragraphs.add(expanded.getText());
          }
        } catch (NoSuchElementException e) {
          LOGGER.warning("Get expanded text not found: " + e.getMessage());
        }

        String para = el.getText();

        if (!(para.isEmpty() || para.contains("Show more") || para.contains("Back to top ↑"))) {
          paragraphs.add(el.getText());
        }
      }

      driver.quit();
    }

    return paragraphs;
  }

  /**
   * Scrape from https://hamilton.govt.nz/privacy-policy
   * @param  driver (WebDriver)
   * @return List<String> paragraphs
   */
  public List<String> fromHamilton(WebDriver driver) {
    List<String> paragraphs = new ArrayList<>();

    // Wait and load main element
    Wait<WebDriver> wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    By condition = By.cssSelector("body > main");
    wait.until(ExpectedConditions.visibilityOfElementLocated(condition));
    WebElement main = driver.findElement(condition);

    List<WebElement> elements = main.findElements(By.cssSelector(".element.elementcontent"));

    for (WebElement el : elements) {
      String para = el.getText();

      if (!(para.isEmpty() || para.contains("Related links"))) {
        paragraphs.add(el.getText());
      }
    }

    driver.quit();

    return paragraphs;
  }

  /**
   * Scrape from https://unsplash.com/privacy
   * @param  driver (WebDriver)
   * @return List<String> paragraphs
   */
  public List<String> fromUnsplash(WebDriver driver) {
    List<String> paragraphs = new ArrayList<>();

    // Wait and load main element
    Wait<WebDriver> wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    By condition = By.cssSelector("#app > div > div");
    // wait until the page fully loaded and testid is changed
    wait.until(ExpectedConditions.attributeToBe(condition, "data-testid", "client-side-hydration-complete"));
    WebElement main = driver.findElement(condition);

    if (main != null) {
      WebElement content = main.findElement(By.cssSelector("div:nth-child(4) > div > div.VSm3O.sZlyb > div > div"));
      paragraphs.add(content.getText());
    }

    driver.quit();

    return paragraphs;
  }

  /**
   * Scrape from https://account.weverse.io/en/policies/cookie-policy
   * @param  driver (WebDriver)
   * @return List<String> paragraphs
   */
  public List<String> fromWeverse(WebDriver driver) {
    List<String> paragraphs = new ArrayList<>();

    // Wait and load main element
    Wait<WebDriver> wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // load next page
    By condition = By.cssSelector("#__next");
    wait.until(ExpectedConditions.visibilityOfElementLocated(condition));
    WebElement main = driver.findElement(condition);

    // load iframe
    By iframeTag = By.tagName("iframe");
    wait.until(ExpectedConditions.visibilityOfElementLocated(iframeTag));
    WebElement iframe = main.findElement(iframeTag);

    if (iframe != null) {
      // Switch to iframe and load element
      driver.switchTo().frame(0);

      By article = By.cssSelector("body > main > div > article");
      wait.until(ExpectedConditions.visibilityOfElementLocated(article));
      WebElement cookies = driver.findElement(article);

      paragraphs.add(cookies.getText());
    }

    driver.quit();

    return paragraphs;
  }

  /**
   * Scrape from https://www.serverless.com/legal/privacy
   * @param  driver (WebDriver)
   * @return List<String> paragraphs
   */
  public List<String> fromServerless(WebDriver driver) {
    List<String> paragraphs = new ArrayList<>();

    Wait<WebDriver> wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    Boolean isChanged = wait.until(ExpectedConditions.urlContains(".pdf"));

    if (isChanged) {
      URL urlOfPdf;
      try {
        urlOfPdf = new URL(driver.getCurrentUrl());
        BufferedInputStream fileToParse = new BufferedInputStream(urlOfPdf.openStream());

        PDDocument document = Loader.loadPDF(RandomAccessReadBuffer.createBufferFromStream(fileToParse));
        String output = new PDFTextStripper().getText(document);
        paragraphs.add(output);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    driver.quit();

    return paragraphs;
  }
}
