package com.ayusuf.sg.tests;

import com.ayusuf.sg.driver.DriverManager;
import com.ayusuf.sg.support.Artifacts;
import com.ayusuf.sg.support.StepTimer;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public abstract class BaseTest {

    protected WebDriver driver;

    @BeforeMethod(alwaysRun = true)
    public void startDriver() {
        StepTimer.start();
        driver = DriverManager.start();
        StepTimer.step("browser started");
    }

    @AfterMethod(alwaysRun = true)
    public void stopDriver(ITestResult result) {
        StepTimer.step("test body done");
        try {
            if (result.getStatus() == ITestResult.FAILURE) {
                String name = result.getMethod().getMethodName();
                System.out.println("Screenshot: " + Artifacts.screenshot(driver, name).toAbsolutePath());
            }
        } catch (RuntimeException e) {
            System.err.println("Could not save screenshot: " + e.getMessage());
        } finally {
            DriverManager.quit();
            StepTimer.step("browser closed");
        }
    }
}
