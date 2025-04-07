package com.snu.testing;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SNULinksTests {

    private static final Logger log = LoggerFactory.getLogger(
        SNULinksTests.class
    );
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
        "yyyyMMdd_HHmmss"
    );

    private WebDriver driver;
    private WebDriverWait wait;
    private String baseURL = "https://snulinks.snu.edu.in/";

    @Before
    public void setUp() {
        log.info("Setting up WebDriver and WebDriverWait...");
        try {
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--remote-allow-origins=*");
            // options.addArguments("--headless=new");
            driver = new ChromeDriver(options);
            log.debug("ChromeDriver initialized.");

            driver.manage().window().maximize();
            wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            log.debug(
                "WebDriverWait initialized with 20 second timeout."
            );

            log.info("Navigating to base URL: {}", baseURL);
            driver.get(baseURL);

            log.debug("Waiting for page body to be present...");
            wait.until(
                ExpectedConditions.presenceOfElementLocated(By.tagName("body"))
            );
            log.info("Base URL loaded and body tag found.");

        } catch (Exception e) {
            log.error("Error during WebDriver setup: {}", e.getMessage(), e);
            if (driver != null) {
                driver.quit();
            }
            fail("WebDriver setup failed: " + e.getMessage());
        }
    }

    // --- Helper Methods (Highlighting & Screenshots) ---

    /**
     * Highlights an element with a specified border color.
     * @param element The WebElement to highlight.
     * @param borderColor CSS color for the border (e.g., "red", "green").
     * @return The original style attribute of the element, or null if highlighting failed.
     */
    private String highlightElement(WebElement element, String borderColor) {
        String originalStyle = null;
        String highlightStyle = String.format(
            "border: 3px solid %s; background: yellow;", // Use borderColor
            borderColor
        );
        try {
            if (element == null) return null;
            JavascriptExecutor js = (JavascriptExecutor) driver;
            originalStyle = (String) js.executeScript(
                "return arguments[0].getAttribute('style');",
                element
            );
            js.executeScript(
                "arguments[0].setAttribute('style', arguments[1]);",
                element,
                highlightStyle // Apply the dynamic style
            );
            log.debug("Highlighted element with {}: {}", borderColor, element);
        } catch (Exception e) {
            log.warn(
                "Could not highlight element with {}: {}",
                borderColor,
                e.getMessage()
            );
        }
        return originalStyle;
    }

    private void unhighlightElement(WebElement element, String originalStyle) {
        try {
            if (element == null) return;
            JavascriptExecutor js = (JavascriptExecutor) driver;
            if (originalStyle != null && !originalStyle.isEmpty()) {
                js.executeScript(
                    "arguments[0].setAttribute('style', arguments[1]);",
                    element,
                    originalStyle
                );
            } else {
                js.executeScript(
                    "arguments[0].removeAttribute('style');",
                    element
                );
            }
            log.debug("Unhighlighted element: {}", element);
        } catch (Exception e) {
            log.warn("Could not unhighlight element: {}", e.getMessage());
        }
    }

    /**
     * Takes a screenshot, optionally highlighting an element first.
     * @param testName Name for the screenshot file.
     * @param elementToHighlight Element to highlight (can be null).
     * @param highlightColor Color for highlighting ("red", "green", etc.). Null if no highlight needed.
     */
    private void takeScreenshot(
        String testName,
        WebElement elementToHighlight,
        String highlightColor
    ) {
        String originalStyle = null;
        // Only highlight if element and color are provided
        if (elementToHighlight != null && highlightColor != null) {
            originalStyle = highlightElement(elementToHighlight, highlightColor);
        }
        if (driver instanceof TakesScreenshot) {
            File scrFile = ((TakesScreenshot) driver).getScreenshotAs(
                OutputType.FILE
            );
            try {
                File screenshotDir = new File("target/screenshots");
                screenshotDir.mkdirs();
                String timestamp = LocalDateTime.now().format(formatter);
                // Add status (PASS/FAIL) to filename if color is provided
                String status = (highlightColor == null)
                    ? ""
                    : (highlightColor.equalsIgnoreCase("green") ? "_PASS" : "_FAIL");
                File destFile = new File(
                    screenshotDir,
                    testName + status + "_" + timestamp + ".png" // Updated filename
                );
                FileUtils.copyFile(scrFile, destFile);
                log.info(
                    "Screenshot saved to: {}",
                    destFile.getAbsolutePath()
                );
            } catch (IOException ioException) {
                log.error(
                    "Failed to save screenshot: {}",
                    ioException.getMessage(),
                    ioException
                );
            } finally {
                // Only unhighlight if element was highlighted
                if (elementToHighlight != null && highlightColor != null) {
                    unhighlightElement(elementToHighlight, originalStyle);
                }
            }
        } else {
            log.warn("Driver does not support taking screenshots.");
        }
    }

    // Overload for taking screenshot on timeout (no highlight)
    private void takeScreenshotOnTimeout(String testName) {
        takeScreenshot(testName, null, null); // No element, no color
    }
    // --- End Helper Methods ---

    @Test
    public void testHomePageTitle() {
        String testName = "testHomePageTitle";
        log.info("Starting test: {}", testName);
        String expectedTitle = "SNULinks";
        log.debug("Waiting for title to be '{}'", expectedTitle);
        try {
            assertTrue(
                "Title did not match expected value within timeout.",
                wait.until(ExpectedConditions.titleIs(expectedTitle))
            );
            String actualTitle = driver.getTitle();
            log.info("Actual title found: '{}'", actualTitle);
            assertEquals(
                "Page title should match",
                expectedTitle,
                actualTitle
            );
            log.info("Test Passed: {}", testName);
            // Take screenshot on PASS (no specific element to highlight for title)
            takeScreenshot(testName, null, "green");
        } catch (Exception e) {
            log.error("Test Failed: {} - {}", testName, e.getMessage(), e);
            takeScreenshotOnTimeout(testName); // Use specific timeout method
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void testLoginLinkIsPresent() {
        String testName = "testLoginLinkIsPresent";
        log.info("Starting test: {}", testName);
        By loginLinkLocator = By.xpath(
            "//a[contains(@class, 'login-btn') or contains(normalize-space(), 'Login')]"
        );
        log.debug("Waiting for Login link visibility: {}", loginLinkLocator);
        WebElement loginLink = null;
        try {
            loginLink = wait.until(
                ExpectedConditions.visibilityOfElementLocated(loginLinkLocator)
            );
            log.info("Login link found and visible.");
            assertTrue("Login link should be displayed", loginLink.isDisplayed());
            log.info("Test Passed: {}", testName);
            // Take screenshot on PASS, highlight green
            takeScreenshot(testName, loginLink, "green");
        } catch (TimeoutException te) {
            log.error(
                "Test Failed (Timeout): {} - {}",
                testName,
                te.getMessage()
            );
            takeScreenshotOnTimeout(testName); // No highlight on timeout
            fail("Test failed (Timeout): " + te.getMessage());
        } catch (Exception e) {
            log.error("Test Failed: {} - {}", testName, e.getMessage(), e);
            // Take screenshot on FAIL, highlight red (if element was found)
            takeScreenshot(testName, loginLink, "red");
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void testUniversityErpLinkIsPresent() {
        String testName = "testUniversityErpLinkIsPresent";
        log.info("Starting test: {}", testName);
        By erpLinkLocator = By.xpath(
            "//a[contains(normalize-space(), 'University ERP')]"
        );
        log.debug(
            "Waiting for University ERP link visibility: {}",
            erpLinkLocator
        );
        WebElement erpLink = null;
        try {
            erpLink = wait.until(
                ExpectedConditions.visibilityOfElementLocated(erpLinkLocator)
            );
            log.info("University ERP link found and visible.");
            assertTrue(
                "University ERP link should be displayed",
                erpLink.isDisplayed()
            );
            log.info("Test Passed: {}", testName);
            // Take screenshot on PASS, highlight green
            takeScreenshot(testName, erpLink, "green");
        } catch (TimeoutException te) {
            log.error(
                "Test Failed (Timeout): {} - {}",
                testName,
                te.getMessage()
            );
            takeScreenshotOnTimeout(testName); // No highlight on timeout
            fail("Test failed (Timeout): " + te.getMessage());
        } catch (Exception e) {
            log.error("Test Failed: {} - {}", testName, e.getMessage(), e);
            // Take screenshot on FAIL, highlight red (if element was found)
            takeScreenshot(testName, erpLink, "red");
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void testFooterAcademicResearchLinkIsPresent() {
        String testName = "testFooterAcademicResearchLinkIsPresent";
        log.info("Starting test: {}", testName);
        By researchLinkLocator = By.linkText("Academic Research");
        log.debug(
            "Waiting for Academic Research link visibility: {}",
            researchLinkLocator
        );
        WebElement researchLink = null;
        try {
            ((JavascriptExecutor) driver).executeScript(
                "window.scrollTo(0, document.body.scrollHeight)"
            );
            log.debug("Scrolled to bottom of page.");

            researchLink = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                    researchLinkLocator
                )
            );
            log.info("Academic Research link found and visible.");
            assertTrue(
                "Academic Research link should be displayed",
                researchLink.isDisplayed()
            );
            log.info("Test Passed: {}", testName);
            // Take screenshot on PASS, highlight green
            takeScreenshot(testName, researchLink, "green");
        } catch (TimeoutException te) {
            log.error(
                "Test Failed (Timeout): {} - {}",
                testName,
                te.getMessage()
            );
            takeScreenshotOnTimeout(testName); // No highlight on timeout
            fail("Test failed (Timeout): " + te.getMessage());
        } catch (Exception e) {
            log.error("Test Failed: {} - {}", testName, e.getMessage(), e);
            // Take screenshot on FAIL, highlight red (if element was found)
            takeScreenshot(testName, researchLink, "red");
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void testFooterCopyrightText() {
        String testName = "testFooterCopyrightText";
        log.info("Starting test: {}", testName);
        By copyrightLocator = By.cssSelector("footer p.text-center.text-white");
        log.debug(
            "Waiting for copyright text presence/visibility: {}",
            copyrightLocator
        );
        WebElement copyrightElement = null;
        try {
            ((JavascriptExecutor) driver).executeScript(
                "window.scrollTo(0, document.body.scrollHeight)"
            );
            log.debug("Scrolled to bottom of page.");

            copyrightElement = wait.until(
                ExpectedConditions.presenceOfElementLocated(copyrightLocator)
            );
            log.info("Copyright element found in DOM.");

            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView(true);",
                copyrightElement
            );
            log.debug("Scrolled copyright element into view.");

            wait.until(ExpectedConditions.visibilityOf(copyrightElement));
            log.info("Copyright element is visible.");

            assertTrue(
                "Copyright text should be displayed",
                copyrightElement.isDisplayed()
            );

            String actualText = copyrightElement.getText().trim();
            log.debug("Actual copyright text: {}", actualText);

            assertTrue(
                "Copyright text does not contain expected content 'Shiv Nadar...'. Found: [" +
                actualText +
                "]",
                actualText.contains(
                    "Shiv Nadar (Institution of Eminence Deemed to be University)"
                )
            );
            assertTrue(
                "Copyright text does not contain the year '2025'. Found: [" +
                actualText +
                "]",
                actualText.contains("2025")
            );
            assertTrue(
                "Copyright text does not contain the '©' symbol. Found: [" +
                actualText +
                "]",
                actualText.contains("©")
            );

            log.info("Test Passed: {}", testName);
            // Take screenshot on PASS, highlight green
            takeScreenshot(testName, copyrightElement, "green");
        } catch (TimeoutException te) {
            log.error(
                "Test Failed (Timeout): {} - {}",
                testName,
                te.getMessage()
            );
            takeScreenshotOnTimeout(testName); // No highlight on timeout
            fail("Test failed (Timeout): " + te.getMessage());
        } catch (Exception e) {
            log.error("Test Failed: {} - {}", testName, e.getMessage(), e);
            // Take screenshot on FAIL, highlight red (if element was found)
            takeScreenshot(testName, copyrightElement, "red");
            fail("Test failed: " + e.getMessage());
        }
    }

    @After
    public void tearDown() {
        log.info("Tearing down WebDriver...");
        if (driver != null) {
            try {
                driver.quit();
                log.info("WebDriver quit successfully.");
            } catch (Exception e) {
                log.error(
                    "Error quitting WebDriver: {}",
                    e.getMessage(),
                    e
                );
            }
        }
    }
}
