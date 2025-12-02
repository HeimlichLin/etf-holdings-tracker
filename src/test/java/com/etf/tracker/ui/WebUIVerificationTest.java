package com.etf.tracker.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.etf.tracker.model.DailySnapshot;
import com.etf.tracker.model.Holding;
import com.etf.tracker.service.ExcelStorageService;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class WebUIVerificationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ExcelStorageService excelStorageService;

    private static Playwright playwright;
    private static Browser browser;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        // Run in headful mode with slow motion to demonstrate the operation
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false)
                .setSlowMo(1000));
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void setupData() {
        // Prepare test data for comparison
        LocalDate date1 = LocalDate.now().minusDays(1);
        LocalDate date2 = LocalDate.now();

        List<Holding> holdings1 = Arrays.asList(
                new Holding("1101", "台泥", 1000L, new BigDecimal("5.0")),
                new Holding("2330", "台積電", 2000L, new BigDecimal("10.0")));
        DailySnapshot snapshot1 = new DailySnapshot(date1, holdings1, holdings1.size(), new BigDecimal("15.0"));

        List<Holding> holdings2 = Arrays.asList(
                new Holding("1101", "台泥", 1000L, new BigDecimal("5.0")), // Same
                new Holding("2330", "台積電", 2500L, new BigDecimal("12.5")), // Increased
                new Holding("2317", "鴻海", 3000L, new BigDecimal("15.0")) // New
        );
        DailySnapshot snapshot2 = new DailySnapshot(date2, holdings2, holdings2.size(), new BigDecimal("32.5"));

        excelStorageService.saveSnapshot(snapshot1);
        excelStorageService.saveSnapshot(snapshot2);
    }

    @Test
    void verifyRangeCompareFeature() {
        try (BrowserContext context = browser.newContext();
                Page page = context.newPage()) {

            page.navigate("http://localhost:" + port);

            // Check for "Range Compare" card (area-compare class)
            Locator compareCard = page.locator(".area-compare");
            // Wait for it to be attached/visible
            compareCard.waitFor();
            assertTrue(compareCard.isVisible(), "Compare card should be visible");

            // Click on the card title to expand the compare section
            Locator compareTitle = compareCard.locator(".card-title");
            compareTitle.click();

            // Wait for the section to expand
            Locator compareSection = page.locator("#compareSection");
            page.waitForFunction("document.getElementById('compareSection').style.display !== 'none'");

            // Check for inputs
            Locator startDate = page.locator("#startDateSelect");
            Locator endDate = page.locator("#endDateSelect");
            Locator compareBtn = page.locator("#compareBtn");

            assertTrue(startDate.isVisible(), "Start date selector should be visible");
            assertTrue(endDate.isVisible(), "End date selector should be visible");
            assertTrue(compareBtn.isVisible(), "Compare button should be visible");

            // Wait for options to be populated
            // The loadDates() function runs on page load.
            // We wait for the select to have options
            page.waitForFunction("document.getElementById('startDateSelect').options.length > 0");

            // Select dates
            String date1 = LocalDate.now().minusDays(1).toString();
            String date2 = LocalDate.now().toString();

            // Ensure the options exist before selecting
            if (startDate.locator("option[value='" + date1 + "']").count() > 0 &&
                    endDate.locator("option[value='" + date2 + "']").count() > 0) {

                startDate.selectOption(date1);
                endDate.selectOption(date2);

                // Click compare
                compareBtn.click();

                // Wait for result
                Locator resultHeader = page.locator("#result h3");
                resultHeader.waitFor();

                String resultText = page.locator("#result").textContent();
                assertTrue(resultText.contains("區間比較"), "Result should contain '區間比較'");
                assertTrue(resultText.contains(date1), "Result should contain start date");
                assertTrue(resultText.contains(date2), "Result should contain end date");

                // Verify specific changes
                assertTrue(resultText.contains("新進"), "Should show new additions");
                assertTrue(resultText.contains("鴻海"), "Should show new stock '鴻海'");
                assertTrue(resultText.contains("增持"), "Should show increased holdings");
                assertTrue(resultText.contains("台積電"), "Should show increased stock '台積電'");

                System.out.println("Web UI Range Compare functionality verified successfully.");

                // Keep browser open for a few seconds to let user see the result
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
            } else {
                System.out.println("Test data dates not found in dropdown. Skipping functional verification.");
                // This might happen if the page loaded before data was saved?
                // But setupData() runs before the test.
                // Maybe the page caches the dates? Or the API call failed?
                // We'll fail the test if this happens to investigate.
                fail("Test data dates (" + date1 + ", " + date2 + ") not found in dropdowns.");
            }
        }
    }
}
