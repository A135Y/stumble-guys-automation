package com.ayusuf.sg.tests;

import com.ayusuf.sg.driver.DriverManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class BaseTest {

    private static final Path ARTIFACTS = Path.of("target", "artifacts",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));

    protected WebDriver driver;

    @BeforeMethod(alwaysRun = true)
    public void startDriver() {
        driver = DriverManager.start();
    }

    @AfterMethod(alwaysRun = true)
    public void stopDriver(ITestResult result) {
        try {
            if (result.getStatus() == ITestResult.FAILURE) {
                screenshot(result.getMethod().getMethodName());
            }
        } finally {
            DriverManager.quit();
        }
    }

    private void screenshot(String name) {
        try {
            Files.createDirectories(ARTIFACTS);
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Path file = ARTIFACTS.resolve(name + ".png");
            Files.write(file, png);
            System.out.println("Screenshot: " + file.toAbsolutePath());
        } catch (IOException | RuntimeException e) {
            System.err.println("Could not save screenshot: " + e.getMessage());
        }
    }
}
